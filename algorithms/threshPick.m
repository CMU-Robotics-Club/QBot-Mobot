function threshPick(I)

figure; imshow(I);
% HSV=rgb2hsv(I);
% I=-HSV(:,:,2);

H = fspecial('gaussian',20,10);
bI = imfilter(I,H);

%% Norm

m = mean(bI(:));
s = std(bI(:));

LINE_THRESH = 2;

bI = (bI - m) / s;

figure; imshow(bI); title('norm');
E = edges(I, 'canny');
figure; imshow(E);

figure; imshow(bI > LINE_THRESH); title('thresh');

figure; imhist(bI);

%% Rand pts

sz = size(bI);
randPts = randi(prod(sz),[1,2000]);

samples = bI(randPts);
linePts = randPts(samples > LINE_THRESH);
[y,x] = ind2sub(sz, linePts);

figure; imagesc(bI); hold on;
plot(x,y,'go'); title('points');

%% Hough

ptImg = zeros(sz);
ptImg(linePts) = 1;
SE = strel('square',3);
ptImg = imdilate(ptImg,SE);
bPtImg = imfilter(ptImg,H);
figure; imagesc(bPtImg);

[H,T,R] = hough(bPtImg);
figure; imshow(H,[],'XData',T,'YData',R,...
            'InitialMagnification','fit');
P = houghpeaks(H);
x = T(P(:,2)); 
y = R(P(:,1));
figure; plot(x,y,'ro');

lines = houghlines(bPtImg,T,R,P);
figure; imshow(bI); hold on;

for k = 1:length(lines)
    xy = [lines(k).point1; lines(k).point2];
    plot(xy(:,1),xy(:,2),'LineWidth',2,'Color','green');

    % plot beginnings and ends of lines
    plot(xy(1,1),xy(1,2),'x','LineWidth',2,'Color','yellow');
    plot(xy(2,1),xy(2,2),'x','LineWidth',2,'Color','red');
end

end