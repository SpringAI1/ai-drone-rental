const fs = require('fs');
const path = require('path');

function createPNG(width, height, pixels) {
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  
  function crc32(data) {
    let crc = 0xFFFFFFFF;
    const table = [];
    for (let i = 0; i < 256; i++) {
      let c = i;
      for (let j = 0; j < 8; j++) {
        c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      }
      table[i] = c;
    }
    for (let i = 0; i < data.length; i++) {
      crc = table[(crc ^ data[i]) & 0xFF] ^ (crc >>> 8);
    }
    return (crc ^ 0xFFFFFFFF) >>> 0;
  }
  
  function createChunk(type, data) {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length);
    const typeBuffer = Buffer.from(type);
    const crcData = Buffer.concat([typeBuffer, data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(crcData));
    return Buffer.concat([length, typeBuffer, data, crc]);
  }
  
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;
  
  const rawData = [];
  for (let y = 0; y < height; y++) {
    rawData.push(0);
    for (let x = 0; x < width; x++) {
      const idx = (y * width + x) * 4;
      rawData.push(pixels[idx], pixels[idx + 1], pixels[idx + 2], pixels[idx + 3]);
    }
  }
  
  const zlib = require('zlib');
  const compressed = zlib.deflateSync(Buffer.from(rawData));
  
  const ihdrChunk = createChunk('IHDR', ihdr);
  const idatChunk = createChunk('IDAT', compressed);
  const iendChunk = createChunk('IEND', Buffer.alloc(0));
  
  return Buffer.concat([signature, ihdrChunk, idatChunk, iendChunk]);
}

function drawIcon(width, height, drawFn) {
  const pixels = new Uint8Array(width * height * 4);
  drawFn(pixels, width, height);
  return createPNG(width, height, pixels);
}

function setPixel(pixels, width, height, x, y, r, g, b, a = 255) {
  if (x >= 0 && x < width && y >= 0 && y < height) {
    const idx = (y * width + x) * 4;
    pixels[idx] = r;
    pixels[idx + 1] = g;
    pixels[idx + 2] = b;
    pixels[idx + 3] = a;
  }
}

function drawCircle(pixels, width, height, cx, cy, radius, r, g, b, a = 255) {
  for (let y = Math.max(0, cy - radius); y <= Math.min(height - 1, cy + radius); y++) {
    for (let x = Math.max(0, cx - radius); x <= Math.min(width - 1, cx + radius); x++) {
      const dx = x - cx;
      const dy = y - cy;
      if (dx * dx + dy * dy <= radius * radius) {
        setPixel(pixels, width, height, x, y, r, g, b, a);
      }
    }
  }
}

function drawRectangle(pixels, width, height, x, y, w, h, r, g, b, a = 255) {
  for (let py = y; py < y + h; py++) {
    for (let px = x; px < x + w; px++) {
      setPixel(pixels, width, height, px, py, r, g, b, a);
    }
  }
}

function drawHomeIcon(color) {
  return drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    const cx = w / 2;
    const roofHeight = 14;
    for (let y = 8; y < 8 + roofHeight; y++) {
      const dy = y - 8;
      const width = roofHeight * 2 - dy * 2;
      const startX = cx - width / 2;
      for (let x = Math.round(startX); x < startX + width; x++) {
        setPixel(pixels, w, h, x, y, color.r, color.g, color.b);
      }
    }
    drawRectangle(pixels, w, h, 10, 20, 28, 22, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 20, 32, 8, 10, color.r, color.g, color.b);
  });
}

function drawDroneIcon(color) {
  return drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    const cx = w / 2, cy = h / 2;
    drawCircle(pixels, w, cx, cy, 8, color.r, color.g, color.b);
    drawCircle(pixels, w, cx, cy, 4, 255, 255, 255);
    drawRectangle(pixels, w, h, cx - 4, cy - 12, 8, 6, color.r, color.g, color.b);
    drawCircle(pixels, w, 10, 14, 5, color.r, color.g, color.b);
    drawCircle(pixels, w, 10, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, 38, 14, 5, color.r, color.g, color.b);
    drawCircle(pixels, w, 38, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, 14, 34, 5, color.r, color.g, color.b);
    drawCircle(pixels, w, 14, 34, 2, 255, 255, 255);
    drawCircle(pixels, w, 34, 34, 5, color.r, color.g, color.b);
    drawCircle(pixels, w, 34, 34, 2, 255, 255, 255);
    drawRectangle(pixels, w, h, 8, 12, 4, 8, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 36, 12, 4, 8, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 10, 30, 4, 8, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 34, 30, 4, 8, color.r, color.g, color.b);
  });
}

function drawOrderIcon(color) {
  return drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 10, 8, 28, 32, color.r, color.g, color.b);
    for (let i = 0; i < 4; i++) {
      drawRectangle(pixels, w, h, 14, 16 + i * 7, 20, 3, 255, 255, 255);
    }
    drawCircle(pixels, w, 34, 18, 4, 255, 255, 255);
    drawCircle(pixels, w, 34, 18, 2, color.r, color.g, color.b);
  });
}

function drawUserIcon(color) {
  return drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    const cx = w / 2;
    drawCircle(pixels, w, cx, 16, 12, color.r, color.g, color.b);
    drawCircle(pixels, w, cx - 4, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, cx + 4, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, cx, 18, 2, 255, 255, 255);
    drawRectangle(pixels, w, h, 14, 28, 20, 18, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 8, 32, 8, 12, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 32, 32, 8, 12, color.r, color.g, color.b);
  });
}

function drawStatsIcons(color) {
  const icons = {};
  
  icons.drone = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, color.r, color.g, color.b, 20);
      }
    }
    const cx = w / 2, cy = h / 2;
    drawCircle(pixels, w, cx, cy, 10, color.r, color.g, color.b);
    drawCircle(pixels, w, cx, cy, 5, 255, 255, 255);
    drawRectangle(pixels, w, h, cx - 5, cy - 14, 10, 6, color.r, color.g, color.b);
    drawCircle(pixels, w, 10, 12, 6, color.r, color.g, color.b);
    drawCircle(pixels, w, 38, 12, 6, color.r, color.g, color.b);
    drawCircle(pixels, w, 14, 36, 6, color.r, color.g, color.b);
    drawCircle(pixels, w, 34, 36, 6, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 7, 10, 5, 9, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 36, 10, 5, 9, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 9, 33, 5, 9, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 34, 33, 5, 9, color.r, color.g, color.b);
  });
  
  icons.user = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, color.r, color.g, color.b, 20);
      }
    }
    const cx = w / 2;
    drawCircle(pixels, w, cx, 16, 14, color.r, color.g, color.b);
    drawCircle(pixels, w, cx - 5, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, cx + 5, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, cx, 20, 2, 255, 255, 255);
    drawRectangle(pixels, w, h, 12, 28, 24, 18, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 6, 32, 10, 14, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 32, 32, 10, 14, color.r, color.g, color.b);
  });
  
  icons.order = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, color.r, color.g, color.b, 20);
      }
    }
    drawRectangle(pixels, w, h, 8, 6, 32, 36, color.r, color.g, color.b);
    for (let i = 0; i < 4; i++) {
      drawRectangle(pixels, w, h, 12, 14 + i * 8, 24, 3, 255, 255, 255);
    }
    drawCircle(pixels, w, 36, 16, 5, 255, 255, 255);
    drawCircle(pixels, w, 36, 16, 2, color.r, color.g, color.b);
  });
  
  icons.money = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, color.r, color.g, color.b, 20);
      }
    }
    drawCircle(pixels, w, 24, 24, 16, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 14, 20, 20, 8, 255, 255, 255);
    drawRectangle(pixels, w, h, 20, 14, 8, 12, color.r, color.g, color.b);
    drawCircle(pixels, w, 24, 14, 4, color.r, color.g, color.b);
  });
  
  return icons;
}

function drawQuickActionIcons(color) {
  const icons = {};
  
  icons.drone_list = drawIcon(64, 64, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 248, 250, 252, 255);
      }
    }
    const cx = w / 2, cy = h / 2;
    drawCircle(pixels, w, cx, cy, 12, color.r, color.g, color.b);
    drawCircle(pixels, w, cx, cy, 6, 255, 255, 255);
    drawRectangle(pixels, w, h, cx - 6, cy - 16, 12, 8, color.r, color.g, color.b);
    drawCircle(pixels, w, 12, 14, 7, color.r, color.g, color.b);
    drawCircle(pixels, w, 52, 14, 7, color.r, color.g, color.b);
    drawCircle(pixels, w, 16, 50, 7, color.r, color.g, color.b);
    drawCircle(pixels, w, 48, 50, 7, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 9, 12, 6, 10, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 49, 12, 6, 10, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 11, 47, 6, 10, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 47, 47, 6, 10, color.r, color.g, color.b);
  });
  
  icons.my_order = drawIcon(64, 64, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 248, 250, 252, 255);
      }
    }
    drawRectangle(pixels, w, h, 10, 8, 44, 48, color.r, color.g, color.b);
    for (let i = 0; i < 4; i++) {
      drawRectangle(pixels, w, h, 16, 18 + i * 10, 32, 4, 255, 255, 255);
    }
    drawCircle(pixels, w, 48, 20, 6, 255, 255, 255);
    drawCircle(pixels, w, 48, 20, 3, color.r, color.g, color.b);
  });
  
  icons.airspace = drawIcon(64, 64, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 248, 250, 252, 255);
      }
    }
    drawRectangle(pixels, w, h, 12, 10, 40, 30, color.r, color.g, color.b);
    for (let i = 0; i < 3; i++) {
      drawRectangle(pixels, w, h, 16, 16 + i * 8, 32, 2, 255, 255, 255);
    }
    drawCircle(pixels, w, 32, 44, 14, color.r, color.g, color.b);
    drawCircle(pixels, w, 32, 44, 8, 255, 255, 255);
    drawRectangle(pixels, w, h, 28, 38, 8, 8, color.r, color.g, color.b);
  });
  
  icons.ai_chat = drawIcon(64, 64, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 248, 250, 252, 255);
      }
    }
    const cx = w / 2, cy = h / 2;
    drawCircle(pixels, w, cx, cy, 18, color.r, color.g, color.b);
    drawCircle(pixels, w, cx - 8, cy - 4, 4, 255, 255, 255);
    drawCircle(pixels, w, cx + 8, cy - 4, 4, 255, 255, 255);
    drawCircle(pixels, w, cx, cy + 4, 4, 255, 255, 255);
    drawRectangle(pixels, w, h, 26, 22, 2, 4, 255, 255, 255);
    drawRectangle(pixels, w, h, 36, 22, 2, 4, 255, 255, 255);
    for (let i = 0; i < 3; i++) {
      drawRectangle(pixels, w, h, 20 + i * 8, 50, 4, 8 + i * 2, color.r, color.g, color.b);
    }
  });
  
  return icons;
}

function drawMenuIcons(color) {
  const icons = {};
  
  icons.my_order = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 8, 6, 32, 36, color.r, color.g, color.b);
    for (let i = 0; i < 4; i++) {
      drawRectangle(pixels, w, h, 12, 14 + i * 8, 24, 3, 255, 255, 255);
    }
    drawCircle(pixels, w, 36, 16, 5, 255, 255, 255);
    drawCircle(pixels, w, 36, 16, 2, color.r, color.g, color.b);
  });
  
  icons.qualification = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 8, 10, 32, 30, color.r, color.g, color.b);
    for (let i = 0; i < 3; i++) {
      drawRectangle(pixels, w, h, 12, 16 + i * 8, 24, 2, 255, 255, 255);
    }
    drawCircle(pixels, w, 32, 38, 8, color.r, color.g, color.b);
    drawCircle(pixels, w, 32, 38, 4, 255, 255, 255);
  });
  
  icons.airspace = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 6, 8, 36, 26, color.r, color.g, color.b);
    for (let i = 0; i < 2; i++) {
      drawRectangle(pixels, w, h, 10, 14 + i * 10, 28, 3, 255, 255, 255);
    }
    drawCircle(pixels, w, 38, 20, 4, color.r, color.g, color.b);
  });
  
  icons.fault = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 6, 18, 12, 22, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 30, 18, 12, 22, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 18, 6, 12, 26, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 14, 34, 20, 6, color.r, color.g, color.b);
  });
  
  icons.ai_chat = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    const cx = w / 2, cy = h / 2;
    drawCircle(pixels, w, cx, cy, 14, color.r, color.g, color.b);
    drawCircle(pixels, w, cx - 6, cy - 3, 3, 255, 255, 255);
    drawCircle(pixels, w, cx + 6, cy - 3, 3, 255, 255, 255);
    drawCircle(pixels, w, cx, cy + 3, 3, 255, 255, 255);
    for (let i = 0; i < 3; i++) {
      drawRectangle(pixels, w, h, 16 + i * 6, 38, 3, 6 + i * 2, color.r, color.g, color.b);
    }
  });
  
  icons.recharge = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawCircle(pixels, w, 24, 24, 14, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 14, 20, 20, 8, 255, 255, 255);
    drawRectangle(pixels, w, h, 20, 14, 8, 10, color.r, color.g, color.b);
    drawCircle(pixels, w, 24, 14, 3, color.r, color.g, color.b);
  });
  
  icons.edit_profile = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    const cx = w / 2;
    drawCircle(pixels, w, cx, 16, 12, color.r, color.g, color.b);
    drawCircle(pixels, w, cx - 4, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, cx + 4, 14, 2, 255, 255, 255);
    drawCircle(pixels, w, cx, 18, 2, 255, 255, 255);
    drawRectangle(pixels, w, h, 14, 28, 20, 16, color.r, color.g, color.b);
    drawCircle(pixels, w, 40, 36, 6, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 36, 32, 6, 6, 255, 255, 255);
    drawRectangle(pixels, w, h, 38, 26, 2, 8, color.r, color.g, color.b);
  });
  
  icons.password = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 8, 12, 32, 28, color.r, color.g, color.b);
    for (let i = 0; i < 4; i++) {
      drawRectangle(pixels, w, h, 12, 18 + i * 6, 24, 3, 255, 255, 255);
    }
    drawRectangle(pixels, w, h, 6, 36, 8, 6, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 34, 36, 8, 6, color.r, color.g, color.b);
    drawCircle(pixels, w, 24, 42, 4, color.r, color.g, color.b);
  });
  
  icons.logout = drawIcon(48, 48, (pixels, w, h) => {
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
      }
    }
    drawRectangle(pixels, w, h, 8, 8, 32, 28, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 14, 14, 20, 4, 255, 255, 255);
    drawCircle(pixels, w, 16, 14, 3, color.r, color.g, color.b);
    drawCircle(pixels, w, 32, 14, 3, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 14, 20, 20, 12, color.r, color.g, color.b);
    drawRectangle(pixels, w, h, 22, 34, 4, 10, color.r, color.g, color.b);
    drawCircle(pixels, w, 24, 44, 4, color.r, color.g, color.b);
  });
  
  return icons;
}

const iconsDir = path.join(__dirname, '../static/icons');
if (!fs.existsSync(iconsDir)) {
  fs.mkdirSync(iconsDir, { recursive: true });
}

const normalColor = { r: 153, g: 153, b: 153 };
const activeColor = { r: 64, g: 158, b: 255 };

fs.writeFileSync(path.join(iconsDir, 'home.png'), drawHomeIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'home-active.png'), drawHomeIcon(activeColor));
fs.writeFileSync(path.join(iconsDir, 'drone.png'), drawDroneIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'drone-active.png'), drawDroneIcon(activeColor));
fs.writeFileSync(path.join(iconsDir, 'order.png'), drawOrderIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'order-active.png'), drawOrderIcon(activeColor));
fs.writeFileSync(path.join(iconsDir, 'user.png'), drawUserIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'user-active.png'), drawUserIcon(activeColor));

const statsBlue = { r: 59, g: 130, b: 246 };
const statsGreen = { r: 34, g: 197, b: 94 };
const statsOrange = { r: 245, g: 158, b: 11 };
const statsPurple = { r: 168, g: 85, b: 247 };

const statsIconsBlue = drawStatsIcons(statsBlue);
const statsIconsGreen = drawStatsIcons(statsGreen);
const statsIconsOrange = drawStatsIcons(statsOrange);
const statsIconsPurple = drawStatsIcons(statsPurple);

fs.writeFileSync(path.join(iconsDir, 'stat-drone.png'), statsIconsBlue.drone);
fs.writeFileSync(path.join(iconsDir, 'stat-user.png'), statsIconsGreen.user);
fs.writeFileSync(path.join(iconsDir, 'stat-order.png'), statsIconsOrange.order);
fs.writeFileSync(path.join(iconsDir, 'stat-money.png'), statsIconsPurple.money);

const actionColor = { r: 64, g: 158, b: 255 };
const actionIcons = drawQuickActionIcons(actionColor);
fs.writeFileSync(path.join(iconsDir, 'action-drone.png'), actionIcons.drone_list);
fs.writeFileSync(path.join(iconsDir, 'action-order.png'), actionIcons.my_order);
fs.writeFileSync(path.join(iconsDir, 'action-airspace.png'), actionIcons.airspace);
fs.writeFileSync(path.join(iconsDir, 'action-chat.png'), actionIcons.ai_chat);

const menuColor = { r: 71, g: 85, b: 105 };
const menuIcons = drawMenuIcons(menuColor);
fs.writeFileSync(path.join(iconsDir, 'menu-order.png'), menuIcons.my_order);
fs.writeFileSync(path.join(iconsDir, 'menu-qualification.png'), menuIcons.qualification);
fs.writeFileSync(path.join(iconsDir, 'menu-airspace.png'), menuIcons.airspace);
fs.writeFileSync(path.join(iconsDir, 'menu-fault.png'), menuIcons.fault);
fs.writeFileSync(path.join(iconsDir, 'menu-chat.png'), menuIcons.ai_chat);
fs.writeFileSync(path.join(iconsDir, 'menu-recharge.png'), menuIcons.recharge);
fs.writeFileSync(path.join(iconsDir, 'menu-edit.png'), menuIcons.edit_profile);
fs.writeFileSync(path.join(iconsDir, 'menu-password.png'), menuIcons.password);
fs.writeFileSync(path.join(iconsDir, 'menu-logout.png'), menuIcons.logout);

const notificationIcon = drawIcon(48, 48, (pixels, w, h) => {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
    }
  }
  drawCircle(pixels, w, h, 24, 24, 16, 255, 255, 255);
  drawCircle(pixels, w, h, 24, 24, 14, 64, 158, 255);
  drawRectangle(pixels, w, h, 20, 18, 8, 8, 255, 255, 255);
  drawCircle(pixels, w, h, 22, 20, 1, 64, 158, 255);
  drawCircle(pixels, w, h, 26, 20, 1, 64, 158, 255);
  drawCircle(pixels, w, h, 24, 24, 1, 64, 158, 255);
});
fs.writeFileSync(path.join(iconsDir, 'notification.png'), notificationIcon);

const searchIcon = drawIcon(48, 48, (pixels, w, h) => {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      setPixel(pixels, w, h, x, y, 255, 255, 255, 0);
    }
  }
  drawCircle(pixels, w, h, 20, 20, 10, 153, 153, 153);
  for (let i = 0; i < 8; i++) {
    const angle = (i / 8) * Math.PI * 2;
    const x = 20 + Math.cos(angle) * 6;
    const y = 20 + Math.sin(angle) * 6;
    drawCircle(pixels, w, h, Math.round(x), Math.round(y), 1, 153, 153, 153);
  }
  for (let i = 0; i < 4; i++) {
    const x = 30 + i;
    const y = 30 + i;
    setPixel(pixels, w, h, x, y, 153, 153, 153);
    setPixel(pixels, w, h, x + 1, y, 153, 153, 153);
  }
});
fs.writeFileSync(path.join(iconsDir, 'search.png'), searchIcon);

console.log('All icons generated successfully!');