<?php
$result = json_encode('');
if($_POST['text']){
    if(file_exists('data.json')){
        unlink('data.json');
    }

    $file = fopen('data.json' , "a+");
    $result = json_encode($_POST['text']);
    fwrite($file , $result);
    fclose($file);
}
?>
<html>
<head>
    <title></title>
    <style>
        .container{
            width: 50%;
            margin : 0 auto;
            padding-top:50px;
            text-align: center;
        }
    </style>
</head>
<body>
<div class="container">
    <form action="index.php" method="post">
        <label for="">Entry</label>
        <br>
        <textarea type="text" id="text" name="text">
        </textarea>
        <br>
        <button type="submit">Submit</button>
    </form>
</div>
</body>
</html>
