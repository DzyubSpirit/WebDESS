env GOOS=windows GOARCH=amd64 go build main.go
mv main.exe main-64.exe
env GOOS=windows GOARCH=386 go build main.go
mv main.exe main-32.exe
