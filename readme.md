# Search Engine Project

## Introduction

This is a search engine implementation with java and tomcat, the following are as project details:
- Step one: crawl website with a spider
    - a spider with BFS approach keep visit new website until all site is fetched
    - the spider obtain its information and content
- Step two: storing and indexing
    - process content which fetch from website by perform stop word removal and stemming
    - store data and keyword in database (we use rocksDb, a key value store db, in our project)
    - build inverted index and keep updating its content
 - Step three: calculate google page rank
    - process all fetched page with page rank algorithm
    - iterate until the page rank of each page is close to converge
    - store the page rank value in database
 - Step four: ready for process query
    - after program receive keyword, do stop word removal and stemming
    - calculate cosine similarity by using the inverted index
    - get the page rank from database
    - calculate score of related page
    - return pages with highest score and render html page with jsp
