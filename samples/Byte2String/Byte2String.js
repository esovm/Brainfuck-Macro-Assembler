var __blockLevel__; __blockLevel__ = 0;
function __indent() { var __result; __result = '';
for (var _i=0 ; _i < __blockLevel__ ; _i++) __result+= '  ';
 return __result;}
var __main; __main=0;
var _main = function(){
 __main++;
var _str_main; 
_str_main='';
if (arguments.length != 0) throw 'Too much arguments provided to macro _main()[' + [] +']';
_str_main+=__indent() +'#    ____        _         ____    ____  _        _' + '\n';
_str_main+=__indent() +'#   | __ ) _   _| |_ ___  |___ \\  / ___|| |_ _ __(_)_ __   __ _' + '\n';
_str_main+=__indent() +'#   |  _ \\| | | | __/ _ \\   __) | \\___ \\| __| \'__| | \'_ \\ / _` |' + '\n';
_str_main+=__indent() +'#   | |_) | |_| | ||  __/  / __/   ___) | |_| |  | | | | | (_| |' + '\n';
_str_main+=__indent() +'#   |____/ \\__, |\\__\\___| |_____| |____/ \\__|_|  |_|_| |_|\\__, |' + '\n';
_str_main+=__indent() +'#          |___/                                          |___/' + '\n';
_str_main+=__indent() +'# Read input byte and output it as decimal number string separated by comma' + '\n';
_str_main+=__indent() +'# this sample does not use macro,' + '\n';
_str_main+=__indent() +'# number are prefixed with \'0\' (e.g 65 is 065, 1 is 001)' + '\n';
_str_main+=__indent() +'' + '\n';
_str_main+=__indent() +'var input' + '\n';
_str_main+=__indent() +'var output*3' + '\n';
_str_main+=__indent() +'' + '\n';
_str_main+=__indent() +'push    1' + '\n';
_str_main+=__indent() +'sbm count' + '\n';
_str_main+=__indent() +'while' + '\n';
__blockLevel__++;
_str_main+=__indent() +'at input' + '\n';
__blockLevel__++;
_str_main+=__indent() +'in' + '\n';
__blockLevel__--;
_str_main+=__indent() +'ta' + '\n';
_str_main+=__indent() +'push count' + '\n';
_str_main+=__indent() +'push 1' + '\n';
_str_main+=__indent() +'equal' + '\n';
_str_main+=__indent() +'if' + '\n';
__blockLevel__++;
_str_main+=__indent() +'at count' + '\n';
__blockLevel__++;
_str_main+=__indent() +'set 2' + '\n';
__blockLevel__--;
_str_main+=__indent() +'ta' + '\n';
__blockLevel__--;
_str_main+=__indent() +'else' + '\n';
__blockLevel__++;
_str_main+=__indent() +'push \',\'' + '\n';
_str_main+=__indent() +'pop out' + '\n';
__blockLevel__--;
_str_main+=__indent() +'fi' + '\n';
_str_main+=__indent() +'push 3' + '\n';
_str_main+=__indent() +'loop' + '\n';
__blockLevel__++;
_str_main+=__indent() +'push input' + '\n';
_str_main+=__indent() +'push 10' + '\n';
_str_main+=__indent() +'div' + '\n';
_str_main+=__indent() +'pop input' + '\n';
_str_main+=__indent() +'pop output' + '\n';
_str_main+=__indent() +'arotl output' + '\n';
__blockLevel__--;
_str_main+=__indent() +'endloop' + '\n';
_str_main+=__indent() +'push 3' + '\n';
_str_main+=__indent() +'sbm cnt' + '\n';
_str_main+=__indent() +'loop' + '\n';
__blockLevel__++;
_str_main+=__indent() +'arotr output' + '\n';
_str_main+=__indent() +'push output' + '\n';
_str_main+=__indent() +'add \'0\'' + '\n';
_str_main+=__indent() +'pop out' + '\n';
__blockLevel__--;
_str_main+=__indent() +'endloop' + '\n';
__blockLevel__--;
_str_main+=__indent() +'wend' + '\n';
return _str_main;
}
_main();
