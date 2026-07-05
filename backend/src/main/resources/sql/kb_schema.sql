-- =====================================================
-- AI 知识库表（增量脚本，独立运行安全）
-- kb_document: 文档元数据
-- kb_chunk:    切块 + embedding (BLOB)
-- =====================================================

CREATE TABLE IF NOT EXISTS kb_document (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,                       -- 上传者；0 表示公共
    scope TEXT NOT NULL DEFAULT 'private',          -- private / public
    filename TEXT NOT NULL,                         -- 原始文件名
    file_type TEXT,                                 -- pdf/docx/xlsx/txt...
    file_size INTEGER DEFAULT 0,                    -- 字节
    file_path TEXT,                                 -- 磁盘路径
    chunk_count INTEGER DEFAULT 0,                  -- 切块数
    char_count INTEGER DEFAULT 0,                   -- 总字符数
    status INTEGER NOT NULL DEFAULT 1,              -- 1=ready / 0=processing / -1=failed
    error_message TEXT,                             -- 解析失败原因
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_kbd_user_id ON kb_document(user_id);
CREATE INDEX IF NOT EXISTS idx_kbd_scope ON kb_document(scope);
CREATE INDEX IF NOT EXISTS idx_kbd_status ON kb_document(status);

-- 切块 + embedding 持久化
-- embedding 存 Base64 字符串（1024 维 float[1024] = 4096 字节 → 5464 字符 base64）
-- 原因：SQLite JDBC 驱动读 BLOB → byte[] 抛 "not implemented by SQLite JDBC driver"
CREATE TABLE IF NOT EXISTS kb_chunk (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER NOT NULL,                   -- 关联 kb_document.id
    chunk_index INTEGER NOT NULL,                   -- 块顺序
    chunk_text TEXT NOT NULL,                       -- 切块原文
    embedding TEXT,                                 -- Base64 编码的 float[1024]
    char_count INTEGER DEFAULT 0,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_kbc_document_id ON kb_chunk(document_id);

-- 知识库访问权限（公共白名单）：user_id 列表可访问 scope=public
-- 当前设计简化：所有登录用户都能访问 scope=public 的文档，故不需要此表
