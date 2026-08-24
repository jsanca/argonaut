# Build the Vue application once, then serve static assets from nginx.
FROM node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . ./
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
# Copy runtime configuration after the compiled bundle so routes can change
# without rebuilding browser assets.
COPY public/config/frameworks.json /usr/share/nginx/html/config/frameworks.json

EXPOSE 80
