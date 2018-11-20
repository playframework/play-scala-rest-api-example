SETUP

Install sbt package (using apt-get).
httpie is used for testing



RUNNING

'sbt run' from terminal



USAGE

Index a book:
http --verbose --json POST http://localhost:9000/v1/book title='Acceptance' author='Jeff' pages[0]='The first page' pages[1]='The second page'

Search for a book page:
 http --verbose http://localhost:9000/v1/book/page?searchPhrase=keyword