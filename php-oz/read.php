<?php
$file = fopen('data.json' , 'r');
$r = file_get_contents('data.json');
$r  = str_replace("\"" , '',$r);
$result = urlencode($result);
fclose($file);
die(json_encode($r));
if(isset($_GET['result'])){
    die(json_encode($r));
}else{
    //header('Location:'."http://localhost:8000/read.php?result=$result");
}


