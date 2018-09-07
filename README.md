# factoid-converters

A java web server to convert [factoid](https://github.com/PathwayCommons/factoid/) documents to BioPAX and SBGN models. 


## Build

```
./gradlew clean
./gradlew build
```

## Run

```
java -jar build/libs/factoid-converters.jar --server.port=8080
```

RESTful API (Swagger docs):

Once the app is built and running, 
the auto-generated documentation is available at 
`http://localhost:8080/factoid-converters/swagger-ui.html`

## Docker
You can deploy the server to a docker container by following the steps below  
(`<PORT>` - actual port number where the server will run). 

```
./gradlew build docker
docker run -it --rm --name factoid-converters -p <PORT>:8080 pathwaycommons/factoid-converters 
```

## Example queries

Using cUrl tool:

```
cd src/test/resources
curl -X POST -H 'Content-Type: application/json' -d @test.json "http://localhost:8080/factoid-converters/v1/json-to-biopax"
```

Using a Node.js client:

```
let url = 'http://localhost:8080/factoid-converters/v1/json-to-biopax';
let content = fs.readFileSync('input/templates.json', 'utf8');
Promise.try( () => fetch( url, {
        method: 'POST',
        body:    content,
        headers: { 'Content-Type': 'application/json' }
    } ) )
    .then( res => res.text() )
    .then( res => {
      // we have biopax result here as a String
      console.log(res);
    } );
```
