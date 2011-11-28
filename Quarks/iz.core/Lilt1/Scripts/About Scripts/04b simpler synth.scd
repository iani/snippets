/* (IZ 2005-08-21) (SC3)
The start function with parameter controls described in the previous example can be automatically generated by sending the message "synth" to the script: 
*/

~script synth: "variable_sin";

/* This will create a start and stop function and store them in the corresponding ~start and ~stop variables. 
The ~controlSpecs variable and the ~start variable can be set in any order inside the script text, if their codes do not explicitly contain variables that depend on each other: 
*/

~controlSpecs = [
[ 'out', 0, 4095, 'linear', 1, 0 ],
[ 'freq', 20, 20000, 'exp', 0, 440 ],
[ 'phase', 0, 6.28319, 'linear', 0, 0 ],
[ 'amp', 0, 1, 'amp', 0, 0.1 ]
];