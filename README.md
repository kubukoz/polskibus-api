# polskibus-api
Polskibus.com API for routes, city search and passage search, written in Scala, served by akka-http.

# Run service

`sbt run`

# Available endpoints

`GET /cities/startingWith/{text}` - returns an array of `City` values whose names begin with {text}, case-insensitive

`GET /targets/{id}` - returns an array of cities you can go to from `City` with `id` equal to {id}.

`GET /passages?from={fromId}&to={toId}&dateStart={dateStart}&adults={adults}[&dateEnd={dateEnd}]` - 

returns a list of `PassageInfo` values corresponding to passages from the city with `id` equal to {fromId} to the city with `id` equal to {toId} on the day specified by {dateStart} (formatted like LocalDateTime.toString) for the {adults} amount of people. Optionally takes a {dateEnd} parameter, but it's unsupported so far.
    
# Roadmap
Kind of none

# Code quality
This is a prototype. It never was supposed to look good in code ;)
