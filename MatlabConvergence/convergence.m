clear;
clc;
close all;

% Analytical Solution
E = 2.07e11;
h = 2;
t = 0.1;
L = 30;

I = t*h^3 / 12;
p = 100;
w = p*L^3 / ( 3 * E * I )
 
%            % 10x1       20x1       %30x1   %20x2      30x2      30x3   30x4
% results = [1.125e-5, 1.673e-5, 1.750e-5, 2.972e-5, 3.562e-5, 4.227e-5, 4.883e-5, ...
%            6.096e-5, 6.192e-5    ]; 
%            %60x4     90x6 
%            
%            
% numOfDof = [44, 84, 124, 126, 186, 248, 310, 610, 1274 ];
% numOfEl  = [20, 40, 60,  80,  120, 180, 240, 480, 1080 ];

           %30x1     %20x2      30x2      30x3      30x4
results = [1.750e-5, 2.972e-5, 3.562e-5, 4.227e-5, 4.883e-5, ...
           6.096e-5, 6.192e-5    ]; 
           %60x4     90x6 
           
           
numOfDof = [124, 126, 186, 248, 310, 610, 1274 ];
numOfEl  = [60,  80,  120, 180, 240, 480, 1080 ];

semilogy(numOfEl, results);
refline(0, w)
xlabel("Number of Elements")
ylabel("u_y");
axis([0, max(numOfEl)+50, 1.5e-5, 7e-5])