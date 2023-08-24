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

## Update log
<br>Update (26/4): term weighted added, code is refactored, phrase search is still not supported.</br>
<br>Update (27/4): posting list of inverted index will record the position of word in the page.
(similar to slide 14 in lecture notes: implementation issues). It can be useful for for identify phrase</br>
<br>Update(29/4): retriever has basic function(calculate cosine similarity), still not support phrase
search. Fixed some bugs in inverted index. Make code cleaner</br>
<br>Update(7/5): retriever has nearly complete function(calculate cosine similarity), support phrase
search. Fixed some bugs. Pre-compute document length, make search faster. Page rank still not supported</br>
<br>Update(10/5): tomcat server was added</br>
<br>Update(11/5): nearly project complete</br>
<br>Update(11/5): added page rank</br>
<br>Update(12/5): clean up code and upgrade CSS
<br>remember to download all the db at the drive</br>
<br>remember to place all db file and stopword.txt at: PATH TO TOMCAT\pache-tomcat-8.5.54-windows-x64\apache-tomcat-8.5.54\bin</br>