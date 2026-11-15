# Storm Browser
A fast, minimal Android browser (WebView) with tabs, private tabs, ad blocking, bookmarks, history, downloads.

## Publish from Termux (APK is built by GitHub Actions)
    pkg update && pkg install git gh -y
    gh auth login
    cd Storm
    git init && git add . && git commit -m "Storm browser"
    git branch -M main
    gh repo create storm-browser --public --source=. --push
    git tag v1.0 && git push origin v1.0

Download `Storm.apk` from the repo's Releases (or Actions > artifacts) and install it.
Note: Private tabs skip history/cache but share cookie storage (WebView limitation).
