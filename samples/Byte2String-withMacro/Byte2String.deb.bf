L#70  : PUSH     1                               
>>>>>>>> >[-]+
L#71  : SBM      count                           
 
L#72  : WHILE                                    
[
L#76  : AT       input                           
<
L#77  : IN                                       
,
>
L#83  : PUSH     STACK(count)                          
>[-]<[>+>+<<-]>[<+>-]>[<+>-]<
L#84  : PUSH     1                               
>[-]+
L#85  : EQUAL                                    
[<->-][-]<[>+<[-]]>[<+>-][-]<[>+
<[-]]>[<+>-]<->[-]<[>+<[-]]>[<+>
-]<
L#86  : IF                                       
>[-]<[>+<[-]]>[<+>-]<[>+>+<<-]>>
[<<+>>-]<[
L#90  : AT       STACK(count)                          
<<
L#91  : SET      2                               
[-]++
>>
-][-]<
L#95  : ELSE                                     
[>+>+<<-]>>[<<+>>-][-]<[>+<[-]]>
[<+>-]<->[-]<[>+<[-]]>[<+>-]<[
L#97  : PUSH     44                               //'/comma/' 
>[-]++++++++++++++++++++++++++++
++++++++++++++++
L#98  : POP      out                             
.[-]<
-][-]<
L#100 : ENDIF                                    
[-]<
L#101 : PUSH     3                               
>[-]+++
L#102 : LOOP                                     
[
L#105 : PUSH     input                           
>[-]<<<[>>>+>+<<<<-]>>>[<<<+>>>-
]>[<+>-]<
L#106 : PUSH     10                              
>[-]++++++++++
L#107 : DIV                                      
[>+>+<<-]>[<+>-] >[<+>-]<<[-]<[-
>+>-[>+>>]>[+[-<+>]>+>>]<<<<<<]>
[-]>[-]>[<<<+>>>-]>[<<<+>>>-]<<<
L#108 : POP      input                           
<<<<[-]>>>>[<<<<+>>>>-]<
L#109 : POP      output                          
<<<<<<<<[-]>>>>>>>>[<<<<<<<<+>>>
>>>>>-]<
L#111 : AROTL    output                          
<<<<<+[>+>+<<-]>>[<<+>>-]+++[-<-
>][-]<[>+<[-]]>[<+>-][-]<[>+<[-]
]>[<+>-]<->[-]<[>+<[-]]>[<+>-]<[
-<[-]>]<<[-]<[->+<]<[->+<]<[->+<
]>>>[-<<<+>>>]>>>>>>
L#112 : ENDLOOP                                  
-][-]<
L#116 : PUSH     1                               
>[-]+
L#117 : SBM      prefixzero                      
 
L#119 : PUSH     3                               
>[-]+++
L#120 : SBM      cnt                             
 
L#121 : LOOP                                     
[
L#122 : AROTR    output                          
<<<<<<[>+>+<<-]>[<+>-]>[<+>-][-]
<[>+<[-]]>[<+>-][-]<[>+<[-]]>[<+
>-]<->[-]<[>+<[-]]>[<+>-]<[-<[-]
+++>]<-<<<<<[-]>[-<+>]>[-<+>]>[-
<+>]<<<[->>>+<<<]>>>>>>>>>>>
L#124 : PUSH     STACK(cnt)                            
>[-]<[>+>+<<-]>[<+>-]>[<+>-]<
L#125 : PUSH     1                               
>[-]+
L#126 : SUP                                      
<+>+>[-]<[>+>+<<-]>[<+>-]>[<+>-]
[-]<[>+<[-]]>[<+>-]<[>+>+<<-]>>[
<<+>>-]<[>[-]<<<<[>>>>+>+<<<<<-]
>>>>[<<<<+>>>>-]>[<+>-][-]<[>+<[
-]]>[<+>-]<[>+>+<<-]>>[<<+>>-]<[
<<<<<->->>>>-][-]<[>+>+<<-]>>[<<
+>>-][-]<[>+<[-]]>[<+>-]<->[-]<[
>+<[-]]>[<+>-]<[<<<<<[-]+>[-]>>>
>-][-]<[-]<-][-]<[>+>+<<-]>>[<<+
>>-][-]<[>+<[-]]>[<+>-]<->[-]<[>
+<[-]]>[<+>-]<[<<<[-]>[-]>>-][-]
<[-]>[-]+>[-]>[-]<<<<<[>>>>>+<<<
<<-]>>>>>>[-]<<<<<[>>>>>+<<<<<-]
>>>>>>[-]<<+>+<[->-[>]<<]<[-]<[-
<]>>>[<<<<<+>>>>>[-]]>[-]<<<<<<
L#127 : IF                                       
>[-]<[>+<[-]]>[<+>-]<[>+>+<<-]>>
[<<+>>-]<[
L#130 : PUSH     output                          
>[-]<<<<<<<<<<<[>>>>>>>>>>>+>+<<
<<<<<<<<<<-]>>>>>>>>>>>[<<<<<<<<
<<<+>>>>>>>>>>>-]>[<+>-]<
L#131 : PUSH     0                               
>[-]
L#132 : DIFF                                     
[<->-][-]<[>+<[-]]>[<+>-]<
L#133 : IF                                       
>[-]<[>+<[-]]>[<+>-]<[>+>+<<-]>>
[<<+>>-]<[
L#136 : PUSH     output                          
>[-]<<<<<<<<<<<<<[>>>>>>>>>>>>>+
>+<<<<<<<<<<<<<<-]>>>>>>>>>>>>>[
<<<<<<<<<<<<<+>>>>>>>>>>>>>-]>[<
+>-]<
L#137 : ADD      48                               //'0' 
++++++++++++++++++++++++++++++++
++++++++++++++++
L#138 : POP      out                             
.[-]<
L#142 : AT       STACK(prefixzero)                     
<<<<<
L#143 : SET      0                               
[-]
>>>>>
-][-]<
L#147 : ELSE                                     
[>+>+<<-]>>[<<+>>-][-]<[>+<[-]]>
[<+>-]<->[-]<[>+<[-]]>[<+>-]<[
L#149 : PUSH     STACK(prefixzero)                     
>[-]<<<<<<[>>>>>>+>+<<<<<<<-]>>>
>>>[<<<<<<+>>>>>>-]>[<+>-]<
L#150 : PUSH     0                               
>[-]
L#151 : EQUAL                                    
[<->-][-]<[>+<[-]]>[<+>-][-]<[>+
<[-]]>[<+>-]<->[-]<[>+<[-]]>[<+>
-]<
L#152 : IF                                       
>[-]<[>+<[-]]>[<+>-]<[>+>+<<-]>>
[<<+>>-]<[
L#155 : PUSH     output                          
>[-]<<<<<<<<<<<<<<<[>>>>>>>>>>>>
>>>+>+<<<<<<<<<<<<<<<<-]>>>>>>>>
>>>>>>>[<<<<<<<<<<<<<<<+>>>>>>>>
>>>>>>>-]>[<+>-]<
L#156 : ADD      48                               //'0' 
++++++++++++++++++++++++++++++++
++++++++++++++++
L#157 : POP      out                             
.[-]<
-][-]<
L#159 : ENDIF                                    
[-]<
-][-]<
L#160 : ENDIF                                    
[-]<
-][-]<
L#161 : ELSE                                     
[>+>+<<-]>>[<<+>>-][-]<[>+<[-]]>
[<+>-]<->[-]<[>+<[-]]>[<+>-]<[
L#163 : PUSH     output                          
>[-]<<<<<<<<<<<[>>>>>>>>>>>+>+<<
<<<<<<<<<<-]>>>>>>>>>>>[<<<<<<<<
<<<+>>>>>>>>>>>-]>[<+>-]<
L#164 : ADD      48                               //'0' 
++++++++++++++++++++++++++++++++
++++++++++++++++
L#165 : POP      out                             
.[-]<
-][-]<
L#167 : ENDIF                                    
[-]<
L#168 : ENDLOOP                                  
-][-]<
L#170 : DROP                                     
[-]<
L#172 : WEND                                     
][-]<
