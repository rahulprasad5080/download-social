# download-social

Android app + local Node.js resolver API for social media media downloads.

## Run backend

```bash
cd backend
npm install
copy .env.example .env
npm run dev
```

The Android app is configured for emulator development with:

```properties
socialDownloaderBaseUrl=http://10.0.2.2:5000/
socialDownloaderEndpoint=api/resolve
socialDownloaderToken=dev-socialhub-token
```

For a physical phone, replace `10.0.2.2` with your computer's LAN IP.
