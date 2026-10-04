const C='stock-v3',A=['/','/manifest.webmanifest','/icon-192.png','/icon-512.png'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(C).then(c=>c.addAll(A)));self.skipWaiting()});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!=C).map(x=>caches.delete(x)))).then(()=>clients.claim()))});
self.addEventListener('fetch',e=>{const r=e.request,u=new URL(r.url);if(r.method!='GET'||u.hostname.endsWith('supabase.co'))return;
if(r.mode=='navigate'){e.respondWith(fetch(r).then(x=>{const cp=x.clone();caches.open(C).then(c=>c.put('/',cp));return x}).catch(()=>caches.match('/')));return}
e.respondWith(caches.match(r).then(m=>m||fetch(r).then(x=>{if(x.ok){const cp=x.clone();caches.open(C).then(c=>c.put(r,cp))}return x})))});
