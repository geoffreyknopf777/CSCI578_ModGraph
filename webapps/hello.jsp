<html>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>

<head>
<title>Sample Application JSP Page</title>
</head>
<body bgcolor=white>

<table border="0">
<tr>
<td align=center>
</td>
<td>
<h1>Sample Application JSP Page</h1>
This is the output of a JSP page that is part of the Hello, World
application.
</td>
</tr>
</table>

<%= new String("Hello!") %>

<% out.write(request.getParameter("repo")); %>
<br> 

<%
  try{
      out.write("Start Clone");
	  Runtime runtime = Runtime.getRuntime();
	  Process exec = runtime.exec("git clone --bare https://github.com/apache/hadoop proj");
	  int i = exec.waitFor();
      out.write("End Clone");
	  out.write("<h1>");
	  System.out.println("Exec Code: " + i);
	  out.write("</h1>");
	  	 
	  exec = runtime.exec("git --no-pager -C /home/cs578user/Desktop/proj/ log -n 10");
	  i = exec.waitFor();
	  BufferedReader in = new BufferedReader( new InputStreamReader(exec.getInputStream()));
	  System.out.println(i);
	  String line;
	  while( (line = in.readLine()) != null){
		  System.out.println(line);
	  }
  } catch(InterruptedException e){
	  throw new RuntimeException(e);
  }
%>

<%= new String("Bye!") %>

<ul>
<li>To a <a href="ModGraph.jsp">JSP page</a>.
</ul>

</body>
</html>