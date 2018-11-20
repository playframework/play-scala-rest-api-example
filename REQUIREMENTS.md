"Boogle"
To Organize the World's Literature



The Why

You've always wanted a way to search book pages easier. Often you found yourself wondering where to find
that great Sherlock Holmes quote or to quickly search for that great explanation you've read in that awesome
tech book last week.

Driven by an overpowering desire to solve your own problem, you created Boogle – the best website to
search book pages in the world. You experienced an almost instant deluge of other book fanatics to your
website, got Tech Crunched and now find yourself staring at a long and sad list of red errors in a log file, at
2am, wondering why your weekend MVP-grade implementation running on a €20 shared server can't cope
with so many thousands of rabid users.

You, however, decide to rise to the challenge: you created something awesome that people love, NOTHING
is going to get in the way of ensuring that all your fans can get their book search goodness on. You make
some coffee, crack your fingers, and get right on to making Boogle soar free. Like an eagle. Or a spaceship.
Whatever, it's 2am.



The Challenge

Build a JSON API to support Boogle's frontend better than the MVP can. The API needs to:
* Allow Books and Book Pages to be indexed for search. Ex: Index the content of Page 34 of The Sign
  of Four so that users can find out how Watson met Sherlock.
* Allow for fast search of Book Pages. Ex: the users will search "watson meets sherlock" and they
  expect to know in which page that happened!
* Allow for Books & Book Pages to be de-indexed, so that they are no longer available for search.
* Allow for Books & Book Pages to be re-indexed, so that their information is updated.

Bear in mind that you:
* Cannot overengineer the solution: it's 2am and Boogle's on fire. You need a good implementation
  that solves your current problem, not a technical tour de force.
*  Pick the right tools for the job. Which databases will you use, if any? How will you write your APIs?
*  Are they fast, easy, available enough?
*  You're a one-person shop (for now). Build something you can run and manage as such.



The Delivery

* Share the code preferably via a github private repository. If that's not possible, please email a
  zipped file.
* Please make small commits, so we can see your progress afterwards
* Place a quick README showing how the app can be setup & run, how the APIs can be manually
  tested (ex: cURL), some test data, etc.
* Tests are welcome, but don't shoot for 100% coverage. One or two would be welcome.
* Be prepared to answer questions about some of your choices!
* If you can't do everything in a reasonable amount of time, try to do one complete function. We'll
  take a look!