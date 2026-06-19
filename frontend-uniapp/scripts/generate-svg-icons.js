const fs = require('fs');
const path = require('path');

const iconsDir = path.join(__dirname, '../static/icons');
if (!fs.existsSync(iconsDir)) {
  fs.mkdirSync(iconsDir, { recursive: true });
}

const homeIcon = (color) => `<?xml version="1.0" encoding="UTF-8"?>
<svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M24 8L8 20V36C8 37.6569 9.34315 39 11 39H17V32H31V39H37C38.6569 39 40 37.6569 40 36V20L24 8Z" fill="${color}"/>
<rect x="18" y="32" width="12" height="7" fill="${color}"/>
</svg>`;

const droneIcon = (color) => `<?xml version="1.0" encoding="UTF-8"?>
<svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
<circle cx="24" cy="24" r="8" fill="${color}"/>
<circle cx="24" cy="24" r="4" fill="white"/>
<rect x="20" y="12" width="8" height="6" fill="${color}"/>
<circle cx="10" cy="14" r="5" fill="${color}"/>
<circle cx="38" cy="14" r="5" fill="${color}"/>
<circle cx="14" cy="34" r="5" fill="${color}"/>
<circle cx="34" cy="34" r="5" fill="${color}"/>
<circle cx="10" cy="14" r="2" fill="white"/>
<circle cx="38" cy="14" r="2" fill="white"/>
<circle cx="14" cy="34" r="2" fill="white"/>
<circle cx="34" cy="34" r="2" fill="white"/>
</svg>`;

const orderIcon = (color) => `<?xml version="1.0" encoding="UTF-8"?>
<svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
<rect x="10" y="8" width="28" height="32" rx="2" fill="${color}"/>
<rect x="14" y="16" width="20" height="3" fill="white"/>
<rect x="14" y="23" width="20" height="3" fill="white"/>
<rect x="14" y="30" width="20" height="3" fill="white"/>
<rect x="14" y="37" width="20" height="3" fill="white"/>
<circle cx="34" cy="18" r="4" fill="white"/>
<circle cx="34" cy="18" r="2" fill="${color}"/>
</svg>`;

const userIcon = (color) => `<?xml version="1.0" encoding="UTF-8"?>
<svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
<circle cx="24" cy="16" r="12" fill="${color}"/>
<circle cx="20" cy="14" r="2" fill="white"/>
<circle cx="28" cy="14" r="2" fill="white"/>
<circle cx="24" cy="18" r="2" fill="white"/>
<rect x="14" y="28" width="20" height="18" rx="2" fill="${color}"/>
<rect x="8" y="32" width="8" height="12" rx="1" fill="${color}"/>
<rect x="32" y="32" width="8" height="12" rx="1" fill="${color}"/>
</svg>`;

const normalColor = '#94a3b8';
const activeColor = '#3b82f6';

fs.writeFileSync(path.join(iconsDir, 'home.svg'), homeIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'home-active.svg'), homeIcon(activeColor));
fs.writeFileSync(path.join(iconsDir, 'drone.svg'), droneIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'drone-active.svg'), droneIcon(activeColor));
fs.writeFileSync(path.join(iconsDir, 'order.svg'), orderIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'order-active.svg'), orderIcon(activeColor));
fs.writeFileSync(path.join(iconsDir, 'user.svg'), userIcon(normalColor));
fs.writeFileSync(path.join(iconsDir, 'user-active.svg'), userIcon(activeColor));

console.log('SVG icons generated successfully!');