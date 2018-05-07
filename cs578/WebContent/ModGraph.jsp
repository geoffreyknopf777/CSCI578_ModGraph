<html>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="ModGraph.FormSubmissionHandler" %>

<head>
<title>Test Mod Application JSP Page</title>
</head>
<body bgcolor=white>

<table border="0">
<tr>
<td align=center>
<img src="images/tomcat.gif">
</td>
<td>
<h1>ModGraph JSP Page</h1>
This is the output of a JSP page that is part of the Hello, World
application.
</td>
</tr>
</table>

<%= new String("Hi again!") %>

<%
  String[] args = {request.getParameter("repo"),request.getParameter("language") };
  FormSubmissionHandler.main(args); 

%>

<%= new String("Bye again!") %>

</body>
</html>
