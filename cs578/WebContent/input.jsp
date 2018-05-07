<html>
    <%@page import="java.io.BufferedReader"%>
    <%@page import="java.io.InputStreamReader"%>
    <head>
        <title>CSCI 578 - Modifiability Analysis from Git Repo Project</title>
        <!-- Required meta tags -->
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, shrink-to-fit=no">
        
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css" integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" crossorigin="anonymous">
        <link rel="stylesheet" type="text/css" href="css/style.css">
        <script src="./js/jquery-3.3.1.min.js "></script>
    </head>
    <body>
        <div id="formDiv" class ="container">
            <p id="formHeader"><b>Modifiability Analysis from Git Repository</b></p>
            <form action="ModGrpah.jsp" method="GET">
                <div>
                    <%= new String("Hello!  Enter the URL of a valid Git Repository to get started!") %></%></br> 
                <%= new String ("For example, try <i> https://github.com/apache/hadoop </i> ") %>
                </div>
                <div class="row form-group">
                    <label for="repoLabel" class="col-form-label col-2">Repository</label>
                    <input type="text" class="form-control col-10" id="repo" name="repo" placehodler="Enter Git URL here">
                </div>
                <div class="form-group row">
                    <label for="language" class="col-2 col-form-label">Language</label>
                    <select class="form-control col-1" name="language" id="language" value="Java">
                        <option id="java">Java</option>
                        <option id="c">C</option>
                    </select>
                </div>
                <div class="row form-group">
                    <button type="submit" class="col-1 offset-2 btn btn-primary">Submit</button>
                </div>
            </form>
        </div>
        
        <div id="progressBar" class="progress">
            <div class="progress-bar progress-bar-striped progress-bar-info progress-bar-animated" role="progressbar" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100" style="width:40%">
            </div>
        </div>
        <script>
            $("#submit").click(function (){$("#progressBar").css("visibility","visible");});
        </script>
        
        <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js" integrity="sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q" crossorigin="anonymous"></script>
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js" integrity="sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl" crossorigin="anonymous"></script>
    </body>
</html>