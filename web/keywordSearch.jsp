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
        h2{
            font-size: 20px;
            font-weight: 500;
        }
        p{
            font-size: 18px;
        }
        .header {
            min-height: 75px;
            padding: 15px;
            text-align: center;
            background-color: #40bad5;
            color: white;
            vertical-align: middle;
        }
        .projectName{
            float: left;
            padding-left: 10px;
            color: #fff;
        }
        .groupName{
            float: right;
            padding-right: 10px;
        }
        .content{
            padding: 25px;
        }
        label{
            font-size: 14px;
            font-family: 'Roboto', sans-serif;
            padding-top: 2.5px;
        }
        .formContent{
            max-height: 500px;
            overflow: auto;
            column-count: 4;
            column-gap: 24px;
        }
        input[type=checkbox]{
            -webkit-appearance: none;
            vertical-align:middle;
            margin-top: 10px;
            margin-bottom: 10px;
            margin-right: 15px;
            background:#fff;
            border:#999 solid 1px;
            border-radius: 3px;
            min-height: 15px;
            min-width: 15px;
            font-size: 14px;
            font-family: 'Roboto', sans-serif;
        }
        input[type=checkbox]:checked {
            background: #04a1bf;
        }
        input[type=checkbox]:checked::after{
            position: absolute;
            content: "";
            left: 15px;
            top: 15px;
            height: 0px;
            width: 0px;
            border-radius: 5px;
            border: solid #04a1bf;
            border-width: 0 3px 3px 0;
            -webkit-transform: rotate(0deg) scale(0);
            -ms-transform: rotate(0deg) scale(0);
            transform: rotate(0deg) scale(0);
            opacity:1;
            transition: all 0.3s ease-out;
            -webkit-transition: all 0.3s ease-out;
            -moz-transition: all 0.3s ease-out;
            -ms-transition: all 0.3s ease-out;
            -o-transition: all 0.3s ease-out;
        }
        input[type=submit] {
            background-color: #40bad5;
            border: none;
            color: white;
            margin: 20px;
            margin-left: 0px;
            padding: 10px 30px;
            width: 125px;
            font-family: 'Roboto', sans-serif;
            font-size: 20px;
            text-decoration: none;
            border-radius: 20px;
            cursor: pointer;
        }
    </style>
</head>
<body>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="indexer.Indexer" %>
<%@ page import="java.util.List" %>
<div class='header'>
    <div class='projectName'>
        <h1>COMP4321 Search Engine Term Project</h1>
    </div>
    <div class='groupName'>
        <h1>Group 9</h1>
    </div>
</div>
<div class="content">
<h2>Keyword Search</h2>
<p>Select keywords from the bottom list and click submit to view the result.</p>
<%
    List<String> keywordList = Indexer.getInstance().getAllStemWord();
    if(keywordList.size() > 0){
        out.println("<form method='post' action='search.jsp'>");
        out.println("<div class='formContent'>");
        for(int i = 0; i < keywordList.size(); i++){
           String keyword = keywordList.get(i);
            if(i > 0){
                out.println("<div><input type='checkbox' id='" + keyword + "' name='selectedWord' value='" + keyword + "'>");
                out.println("<label class='checkbox-label' for='" + keyword + "'>" + keyword + "</label></div>");
            }
        }
        out.println("</div>");
        out.println("<input type='submit' value='Submit'>");
        out.println("</form");
    }
%>
</div>
</body>
</html>