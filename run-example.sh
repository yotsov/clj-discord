git status
git pull origin master
lein clean
lein install
cd example
lein clean
lein uberjar
java -jar target/clj-discord-example-0.1.0-SNAPSHOT-standalone.jar
