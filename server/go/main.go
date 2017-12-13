package main

import (
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"os/exec"
)

func simulation(resp http.ResponseWriter, req *http.Request) {
	log.Println("request start")
	cmd := exec.Command("java", "-classpath", "ParallelDESS.jar", "cli.NetSimulator")
	stdin, err := cmd.StdinPipe()
	if err != nil {
		log.Printf("ERROR: cmd.StdinPipe(): %v", err)
		return
	}

	stderr, err := cmd.StderrPipe()
	if err != nil {
		log.Printf("ERROR: cmd.StderrPipe(): %v", err)
		return
	}
	var errOutput []byte
	go func() {
		errOutput, err = ioutil.ReadAll(stderr)
		if err != nil {
			log.Printf("can't read err output: %v", errOutput)
		}
	}()

	resp.Header().Set("Access-Control-Allow-Origin", "*")
	resp.Header().Set("Content-Type", "*")

	log.Println("Copy to stdin")
	_, err = io.Copy(stdin, req.Body)
	if err != nil {
		log.Printf("ERROR: io.Copy(): %v", err)
		return
	}
	log.Println("Run")
	output, err := cmd.Output()
	if err != nil {
		log.Printf("ERROR: cmd.Output(): %v: %q, stderr: %q", err, string(output), string(errOutput))
		return
	}

	log.Println("Copy from stdout")

	_, err = resp.Write(output)
	if err != nil {
		log.Printf("ERROR: resp.Write(): %v", err)
		return
	}

	log.Println("request end")
}

func main() {
	http.Handle("/", http.FileServer(http.Dir("../../public_html")))
	http.HandleFunc("/simulator", simulation)

	log.Println("Server has been started on http://localhost:8080/")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
