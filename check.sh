# format the code
lein cljfmt fix
lein cljfmt fix project.clj

# check for updatable dependencies and for dependency conflicts
lein ancient
lein ancient :plugins
lein deps :tree > /dev/null # we are only interested in stderr
lein deps :plugin-tree > /dev/null # we are only interested in stderr

# run the linters
touch token.txt
lein eastwood
rm token.txt
lein kibit
