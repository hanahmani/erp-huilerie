# ---------- Étape 1 : build Vite ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci || npm install
COPY . .
ENV VITE_API_URL=/api
RUN npm run build

# ---------- Étape 2 : Nginx sert les fichiers et relaie /api ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
