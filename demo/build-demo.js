#!/usr/bin/env node
/*
 * Builds the static SkillVerse demo into demo/dist from the real front end in web/.
 *
 *   node demo/build-demo.js
 *
 * - copies web/ unchanged,
 * - injects the mock API + seed data as the first scripts of every page,
 * - removes the real PayHere SDK tag (the demo ships a fake sandbox dialog),
 * - writes the access-gate config, a gate page and robots.txt.
 *
 * Optional environment variables (set them in the Vercel project settings):
 *   DEMO_ACCESS_TOKEN    secret string. When set, the demo only opens via  /?access=<token>
 *                        or when the visitor arrives from an allowed host.
 *   DEMO_ALLOWED_HOSTS   comma-separated referrer hosts, default "dhanikaj.dev"
 *   DEMO_GATE            "referrer" to gate on referrer alone (no token), "off" to disable
 *   DEMO_PORTFOLIO_URL   where the gate page sends people, default https://dhanikaj.dev
 * With none of these set the demo is open to anyone who has the URL.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const webDir = path.join(root, 'web');
const srcDir = path.join(__dirname, 'src');
const outDir = path.join(__dirname, 'dist');

// Empty the folder rather than deleting it, so a dev server running inside it does not block a rebuild.
fs.mkdirSync(outDir, {recursive: true});
for (const entry of fs.readdirSync(outDir)) {
    fs.rmSync(path.join(outDir, entry), {recursive: true, force: true});
}
fs.cpSync(webDir, outDir, {recursive: true});
fs.rmSync(path.join(outDir, 'WEB-INF'), {recursive: true, force: true}); // server config, not for a static host

// ---- scripts -----------------------------------------------------------------
const jsDir = path.join(outDir, 'assets', 'js', 'demo');
fs.mkdirSync(jsDir, {recursive: true});
fs.copyFileSync(path.join(srcDir, 'seed-data.js'), path.join(jsDir, 'seed-data.js'));
fs.copyFileSync(path.join(srcDir, 'mock-api.js'), path.join(jsDir, 'mock-api.js'));

// ---- access gate config -------------------------------------------------------
const token = (process.env.DEMO_ACCESS_TOKEN || '').trim();
const mode = (process.env.DEMO_GATE || '').trim().toLowerCase();
const gate = mode === 'off' ? false : (token !== '' || mode === 'referrer');
const allowedHosts = (process.env.DEMO_ALLOWED_HOSTS || 'dhanikaj.dev')
        .split(',').map((h) => h.trim().toLowerCase()).filter(Boolean);
const portfolioUrl = process.env.DEMO_PORTFOLIO_URL || 'https://dhanikaj.dev';

fs.writeFileSync(path.join(outDir, 'demo-config.js'),
        'window.SV_DEMO_CONFIG = ' + JSON.stringify({gate, token, allowedHosts}) + ';\n');

// ---- inject into every top-level page ---------------------------------------
const snippet = '\n<script src="demo-config.js"></script>'
        + '\n<script src="assets/js/demo/seed-data.js"></script>'
        + '\n<script src="assets/js/demo/mock-api.js"></script>\n';

let pages = 0;
for (const file of fs.readdirSync(outDir)) {
    if (!file.endsWith('.html')) {
        continue;
    }
    const full = path.join(outDir, file);
    let html = fs.readFileSync(full, 'utf8');
    html = html.replace(/<script[^>]*payhere\.lk[^>]*>\s*<\/script>/gi, '');
    // Fragments (navbar, footer, cart drawer) are fetched into pages, not loaded alone.
    const isFragment = !/<head[\s>]/i.test(html);
    if (!isFragment) {
        html = html.replace(/<head[^>]*>/i, (m) => m + snippet);
        pages++;
    }
    fs.writeFileSync(full, html);
}

// ---- gate page + robots -------------------------------------------------------
fs.writeFileSync(path.join(outDir, 'demo-gate.html'), `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow"><title>SkillVerse demo</title>
<style>body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0f172a;
color:#e5e7eb;font-family:Arial,Helvetica,sans-serif;padding:24px}
.card{max-width:440px;background:#1e293b;border-radius:16px;padding:32px;text-align:center}
a{display:inline-block;margin-top:16px;background:#006bff;color:#fff;text-decoration:none;padding:10px 18px;border-radius:10px;font-weight:700}
p{color:#94a3b8;line-height:1.5}</style></head>
<body><div class="card"><h2>SkillVerse live demo</h2>
<p>This demo is opened from my portfolio. Use the link there and it will load straight away.</p>
<a href="${portfolioUrl}">Go to the portfolio</a></div></body></html>
`);
fs.writeFileSync(path.join(outDir, 'robots.txt'), 'User-agent: *\nDisallow: /\n');

console.log(`Demo built: ${pages} pages -> ${path.relative(root, outDir)}`
        + ` (gate ${gate ? 'ON' + (token ? ' + token' : ' (referrer only)') : 'OFF'})`);
