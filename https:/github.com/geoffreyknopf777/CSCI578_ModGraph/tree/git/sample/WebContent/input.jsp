<html>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>

<head>
<title>Input Application JSP Page</title>
</head>
<body bgcolor=white>

<table border="0">
<tr>
<td align=center>
<img src="images/tomcat.gif">
</td>
<td>
<h1>Get input for NFP repo</h1>
This is the output of a JSP page that is part of getting user input.
</td>
</tr>
</table>

<%= new String("Hello!") %>
<%= new String ("Try https://github.com/apache/hadoop") %>
      <form action = "ModGraph.jsp" method = "GET">
         REPO: <input type = "text" name = "repo">
          <br />
         Language : <input type = "text" name = "lang" /> 
         <br />  
         <input type = "submit" value = "Submit" />
      </form>

<%= new String("Bye!") %>

</body>
</html>
