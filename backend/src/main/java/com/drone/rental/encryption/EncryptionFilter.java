package com.drone.rental.encryption;

import com.drone.rental.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 全局加密/解密过滤器（信封加密）
 * <p>
 * 功能：
 * 1. 识别请求是否为加密请求 (包含 encryptedKey + encryptedData)
 * 2. 若是 -> 用 RSA 私钥解出 AES key，再用 AES 解出业务 JSON，重新包装请求
 * 3. 包装响应输出流，将响应 JSON 加密后再发送给前端
 * <p>
 * 注意：
 * - /public/* 等公开接口不加密（保持向后兼容，前端也能获取 RSA 公钥本身）
 * - 若前端未加密（明文请求），则后端也不加密响应 → 支持渐进式接入
 * - Order=1：必须比 JwtInterceptor 早执行，否则鉴权拦截器读不到解密后的 body
 */
@Slf4j
@Component
@Order(1)
public class EncryptionFilter implements Filter {

    @Autowired
    private EncryptionService encryptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 公开接口（/public/**）、文件上传、OPTIONS、Swagger 跳过加密
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (isSkipEncryption(uri, method)) {
            chain.doFilter(request, response);
            return;
        }

        // 读取原始请求体
        CachedBodyRequest wrappedRequest = new CachedBodyRequest(httpRequest);
        String rawBody = wrappedRequest.getCachedBody();

        boolean encrypted = false;
        String aesKeyBase64 = null;

        if (StringUtils.hasText(rawBody) && rawBody.contains("\"encryptedKey\"") && rawBody.contains("\"encryptedData\"")) {
            // 这是一个加密请求，尝试解密
            try {
                EncryptedRequest enc = objectMapper.readValue(rawBody, EncryptedRequest.class);
                if (enc.getEncryptedKey() != null && enc.getEncryptedData() != null) {
                    // 1) RSA 解密 AES key
                    aesKeyBase64 = encryptionService.rsaDecrypt(enc.getEncryptedKey());
                    // 2) AES 解密业务数据
                    String plainJson = encryptionService.aesDecrypt(enc.getEncryptedData(), aesKeyBase64);
                    // 3) 用明文 JSON 包装请求，后面 controller 读到的就是明文
                    wrappedRequest.setBody(plainJson);
                    // 4) 将 AES key 绑定到线程变量，用于加密响应
                    encryptionService.bindAesKey(aesKeyBase64);
                    encrypted = true;
                }
            } catch (Exception e) {
                log.warn("解密请求失败，当作普通请求处理: {}", e.getMessage());
                encrypted = false;
            }
        }

        try {
            if (encrypted) {
                // 包装响应输出流，以便在最后加密
                CachedBodyResponse wrappedResponse = new CachedBodyResponse(httpResponse);
                chain.doFilter(wrappedRequest, wrappedResponse);

                // 加密响应体
                byte[] responseBytes = wrappedResponse.getCachedBody();
                String responseJson = new String(responseBytes, StandardCharsets.UTF_8);
                try {
                    String cipherText = encryptionService.aesEncrypt(responseJson, aesKeyBase64);
                    // 组装加密响应结构
                    String encryptedResponse = "{\"encrypted\":true,\"data\":\"" + escapeJson(cipherText) + "\"}";
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.setContentLength(encryptedResponse.getBytes(StandardCharsets.UTF_8).length);
                    httpResponse.getOutputStream().write(encryptedResponse.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.error("加密响应失败", e);
                    httpResponse.getOutputStream().write(responseBytes);
                }
            } else {
                chain.doFilter(wrappedRequest, response);
            }
        } finally {
            if (encrypted) {
                encryptionService.clearAesKey();
            }
        }
    }

    /**
     * 判断是否跳过加密
     */
    private boolean isSkipEncryption(String uri, String method) {
        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        if (uri == null) return true;

        // 公开接口：允许前端拿到公钥（公钥本身是公开信息，不需要加密）
        if (uri.contains("/public/")) return true;

        // 文件上传：multipart/form-data 不做 JSON 加密
        if (uri.contains("/common/upload") || uri.contains("/uploads/")) return true;

        // Swagger / OpenAPI 文档
        if (uri.contains("/v3/api-docs") || uri.contains("/swagger-ui/")
                || uri.contains("/doc.html") || uri.contains("/api-docs")) return true;

        // WebSocket 握手
        if (uri.contains("/ws/") || uri.contains("/ws-")) return true;

        // 静态资源
        if (uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".svg")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".jpeg")
                || uri.endsWith(".ico") || uri.endsWith(".html")) return true;

        return false;
    }

    /**
     * 简单的 JSON 字符串转义（针对 Base64 字符，只需处理 \ 和 "）
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (char c : s.toCharArray()) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else sb.append(c);
        }
        return sb.toString();
    }

    // ============ 内部工具类：缓存请求/响应体的包装器 ============

    /**
     * 包装 HttpServletRequest，允许读取 body 并替换 body
     */
    static class CachedBodyRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private byte[] cachedBody;
        private ServletInputStream inputStream;

        public CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        public String getCachedBody() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }

        public void setBody(String newBody) {
            this.cachedBody = newBody.getBytes(StandardCharsets.UTF_8);
            this.inputStream = null; // 重置，下次 getInputStream 重新创建
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStream == null) {
                final ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
                inputStream = new ServletInputStream() {
                    @Override
                    public int read() { return bais.read(); }
                    @Override
                    public boolean isFinished() { return bais.available() == 0; }
                    @Override
                    public boolean isReady() { return bais.available() > 0; }
                    @Override
                    public void setReadListener(ReadListener readListener) { }
                };
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return cachedBody.length; }
        @Override
        public long getContentLengthLong() { return cachedBody.length; }
    }

    /**
     * 包装 HttpServletResponse，缓存输出流以便后续加密
     */
    static class CachedBodyResponse extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final ByteArrayOutputStream cachedBody = new ByteArrayOutputStream();
        private final ServletOutputStream outputStream;
        private PrintWriter writer;

        public CachedBodyResponse(HttpServletResponse response) {
            super(response);
            outputStream = new ServletOutputStream() {
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setWriteListener(WriteListener writeListener) { }
                @Override
                public void write(int b) { cachedBody.write(b); }
                @Override
                public void write(byte[] b, int off, int len) { cachedBody.write(b, off, len); }
            };
        }

        public byte[] getCachedBody() {
            try {
                if (writer != null) writer.flush();
                outputStream.flush();
            } catch (Exception ignored) {}
            return cachedBody.toByteArray();
        }

        @Override
        public ServletOutputStream getOutputStream() { return outputStream; }

        @Override
        public PrintWriter getWriter() throws UnsupportedEncodingException {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(cachedBody, StandardCharsets.UTF_8), true);
            }
            return writer;
        }
    }
}
