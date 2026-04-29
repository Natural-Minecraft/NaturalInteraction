// Minecraft color code → CSS color
const MC = {
  '0':'#000','1':'#0000AA','2':'#00AA00','3':'#00AAAA','4':'#AA0000',
  '5':'#AA00AA','6':'#FFAA00','7':'#AAAAAA','8':'#555555','9':'#5555FF',
  'a':'#55FF55','b':'#55FFFF','c':'#FF5555','d':'#FF55FF','e':'#FFFF55','f':'#FFF',
};

export function parseMCText(raw = '') {
  const spans = [];
  let color = '#e6edf3', bold = false, italic = false;
  for (let i = 0; i < raw.length; i++) {
    if (raw[i] === '&' && i + 1 < raw.length) {
      const c = raw[i + 1].toLowerCase();
      if (MC[c]) { color = MC[c]; i++; continue; }
      if (c === 'l') { bold = true; i++; continue; }
      if (c === 'o') { italic = true; i++; continue; }
      if (c === 'r') { color = '#e6edf3'; bold = false; italic = false; i++; continue; }
    }
    spans.push(<span key={i} style={{
      color, fontWeight: bold ? 700 : 400, fontStyle: italic ? 'italic' : 'normal'
    }}>{raw[i]}</span>);
  }
  return spans;
}

export function stripMC(text = '') {
  return text.replace(/&[0-9a-flmnorkrR]/g, '');
}
