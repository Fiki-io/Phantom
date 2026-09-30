import urllib.request, json, re

req = urllib.request.Request(
    'https://www.youtube.com/embed/W20W3qJ_u7g',
    headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0'}
)
html = urllib.request.urlopen(req).read().decode('utf-8')
m = re.search(r'\"embedded_player_response\":(\"[^\"]+\")', html)
if m:
    raw = json.loads(m.group(1))
    data = json.loads(raw)
    print("Keys:", list(data.keys()))
    print("storyboards in embedded_player_response:", "storyboards" in data)
else:
    print("embedded_player_response not found")
