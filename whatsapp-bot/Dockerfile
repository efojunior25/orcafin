FROM node:20-slim

# Chromium do próprio Debian em vez do bundled do Puppeteer — mais confiável
# de instalar em container (evita o download interno do Puppeteer falhar
# por dependência de sistema faltando).
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xdg-utils \
    libu2f-udev \
    libvulkan1 \
    && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

WORKDIR /app

RUN groupadd -r app && useradd -r -g app -m app && mkdir -p /data/session && chown -R app:app /data /app

COPY package.json ./
RUN npm install --omit=dev

COPY index.js ./
RUN chown -R app:app /app

USER app

CMD ["node", "index.js"]
