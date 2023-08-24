<html>
<head>
	<title>COMP4321 Search Engine</title>
	<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
	<link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
	<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap" rel="stylesheet">
	<style>
		html, body {
			font-family: 'Roboto', sans-serif;
			margin: 0;
			padding: 0;
			overflow:initial !important;
		}
		h1{
			font-size: 22px;
		}
		a{
			text-decoration: none;
		}
		.search{
			font-size: 16px;
			border: 2px solid #f0f0f0;
			border-radius: 25px;
			margin: 15px;
			padding: 15px;
			vertical-align: middle;
			min-height: 60px;
		}
		.icon, .projectName, #result_count, .modTime{
			float: left;
		}
		.projectName{
			padding-left: 10px;
			color: #fff;
		}
		.groupName{
			float: right;
			padding-right: 10px;
		}
		.input{
			padding-left: 30px;
			font-size: 16px;
			vertical-align: middle;
		}
		.icon, .input{
			color: #757575;
		}
		.icon{
			padding-top: 5px;
		}
		.header {
			min-height: 75px;
			padding: 15px;
			text-align: center;
			background-color: #40bad5;
			color: white;
			vertical-align: middle;
		}
		#result_count{
			padding: 15px 25px 0px 25px;
			color: #70757a;
		}
		.content{
			padding: 25px;
		}
		.s{
			vertical-align: top;
			padding-top: 12.5px;
			padding-right: 25px;
		}
		.page{
			padding: 7.5px 0 7.5px 0;
		}
		.url{
			color: #424142;
			font-size: 12px;
		}
		.title{
			color: #035aa6;
			font-size: 20px;
			text-transform: capitalize;
		}
		.modTime, .size{
			color:rgb(112, 117, 122);
			font-size: 14px;
		}
		.size{
			padding-left: 15px;
			display: inline-block;
		}
		.keyword{
			font-size: 12px;
			color: #424142;
		}
		.link{
			font-size: 12px;
			color: #424142;
			max-height: 200px;
			overflow: auto;
		}
		#links{
			font-size: 14px;
			padding-top: 5px;
		}
		.w3-button {
			margin: 5px;
			padding: 7.5px;
			width:150px;
			font-size: 16px;
			border-radius: 15px;
			transition: 0.8s;
		}
		.w3-button:hover{
			color: mediumturquoise;
			box-shadow: 5px 5px 5px grey;
		}
	</style>
</head>
<body>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="indexer.InvertedIndex" %>
<%@ page import="indexer.PageProperty" %>
<%@ page import="retriever.PreProcessor" %>
<%@ page import="retriever.Retrieval" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>

<%

if(request.getParameter("input")!=null || request.getParameterValues("selectedWord")!=null)
{
	out.println("<div class='header'><div class='projectName'><h1>COMP4321 Search Engine Term Project</h1></div><div class='groupName'><h1>Group 9</h1></div></div>");
	String query = "";
	if(request.getParameter("input") != null){
		 query = request.getParameter("input");
	}
	else{
		String[] values = request.getParameterValues("selectedWord");
		if(values.length == 1)
			query = values[0];
		else {
			for (String val : values) {
				query = query.concat(val + " ");
			}
		}
	}
	//query = request.getParameter("input");
	out.println("<div class='search'><div class='icon'><i class=\"fa fa-search\"></i></div>" +
			"<div class='input'>" + query + "</div></div>");

	double startTime = System.nanoTime();
    Retrieval retrieval = new Retrieval(query);
    double duration = (System.nanoTime() - startTime) / 1000000000;
	LinkedHashMap<Integer, Double> result = retrieval.getResult();

	int result_no = result.size();
	out.println("<div id='result_count'>Number of search results: " + result_no + " takes " + duration + "s " + "</div><div id='button'><button class=\"w3-button w3-blue\"><a href=\"index.html\">Search Again</a></button></div>");

	if(result.size() > 0){
		out.println("<div class='content'><table>");
		for(Map.Entry<Integer, Double> resultEntry: result.entrySet()){
			Integer id = resultEntry.getKey();
			LinkedHashMap<String, Integer> sortedWordList = InvertedIndex.getInstance().getSortedWordFreqWordList(id);
			double doc_score = (double)Math.round(resultEntry.getValue() * 100000d) / 100000d;
			String url = PageProperty.getInstance().getUrl(id);
			String title = PageProperty.getInstance().getTitle(id);
			String mod_time = PageProperty.getInstance().getLastModificationTime(id);
			String size = PageProperty.getInstance().getSize(id);
			String ChildLinks = InvertedIndex.getInstance().getChildPages(id);
			String ParentLinks = PreProcessor.getInstance().getParentPages(id);
			out.println("<tr>");
			out.println("<td class='s' rowspan='4'><div class='score'>"+ doc_score + "</div></td>");
			out.println("<td><div class='page'><div class='title'><a href='" + url + "'>" + title + "</a></div><div class='url'>" + url + "</div><div class='modTime'>" + mod_time + "</div><div class='size'>" + size + "</div></div></td>");
			out.println("</tr>");
			out.println("<tr><td><div class='keyword'>");
			int count = 0;
			for (Map.Entry<String, Integer> wordEntry : sortedWordList.entrySet()) {
				if (count++ > 5) {
					break;
				}
				out.println(wordEntry.getKey() + " " + wordEntry.getValue() + "; ");
			}
			out.println("</div></td></tr>");
			out.println("<tr><td>");
			String[] parent_links = ParentLinks.split("\n");
			out.println("<span id='links'>Parent Links: </span><br><div class='link'>");
            for (String link : parent_links) { out.println(link + "<br>"); }
			out.println("</td></tr>");
			out.println("<tr><td>");
			String[] child_links = ChildLinks.split("\n");
			if(child_links.length > 1) {
				out.println("<span id='links'>Child Links: </span><br><div class='link'>");
				for (String link : child_links) {
					out.println(link + "<br>");
				}
			}
			out.println("</div></td></tr>");
		}
		out.println("</table></div>");
	}else{
		out.println("<div class='content'>No match result</div>");
	}
}
else
{
	out.println("You input nothing");
}

%>
</body>
</html>