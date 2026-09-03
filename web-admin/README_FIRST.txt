EchoSnap 관리자 웹 - 깨끗한 교체본

중요:
1. 기존 C:\workspace\EchoSnap\web-admin 폴더는 이름을 web-admin-old 로 변경하세요.
2. 이 ZIP의 web-admin 폴더를 C:\workspace\EchoSnap\ 아래에 넣으세요.
3. Git Bash에서:
   cd /c/workspace/EchoSnap/web-admin
   rm -rf node_modules package-lock.json
   npm i
4. esbuild install script blocked 경고가 나오면:
   npm install-scripts approve esbuild
   npm rebuild esbuild
5. npm run build

이 프로젝트는 axios / zustand / react-query / sonner / react-hook-form 등을 사용하지 않습니다.
src 폴더에는 현재 관리자 구현에 필요한 파일만 들어 있습니다.
