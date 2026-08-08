const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios = require('axios');

const BACKEND_URL = process.env.BACKEND_URL;
const INTERNAL_API_KEY = process.env.INTERNAL_API_KEY;

if (!BACKEND_URL || !INTERNAL_API_KEY || INTERNAL_API_KEY.length < 24) {
  console.error('BACKEND_URL e INTERNAL_API_KEY (>=24 chars) são obrigatórios.');
  process.exit(1);
}

const http = axios.create({
  baseURL: BACKEND_URL,
  timeout: 30000,
  headers: { 'X-Internal-Api-Key': INTERNAL_API_KEY },
});

async function callBackend(path, body) {
  try {
    const { data } = await http.post(path, body);
    return data.message || 'Ok.';
  } catch (err) {
    const backendMessage = err.response && err.response.data && err.response.data.message;
    console.error(`Erro chamando ${path}:`, err.response ? err.response.status : err.message);
    return backendMessage || 'Deu um erro por aqui. Tenta de novo em instantes.';
  }
}

const client = new Client({
  authStrategy: new LocalAuth({ dataPath: '/data/session' }),
  puppeteer: {
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  },
});

client.on('qr', (qr) => {
  console.log('Escaneie o QR code abaixo com o WhatsApp do número que vai virar o bot:');
  qrcode.generate(qr, { small: true });
});

client.on('ready', () => {
  console.log('OrçaFin WhatsApp bot conectado e pronto.');
});

client.on('auth_failure', (msg) => {
  console.error('Falha de autenticação no WhatsApp:', msg);
});

client.on('disconnected', (reason) => {
  console.error('WhatsApp desconectado:', reason);
});

const CONFIRM_YES = new Set(['1', 'sim', 'confirmar', 'confirma']);
const CONFIRM_NO = new Set(['2', 'nao', 'não', 'cancelar', 'cancela']);

client.on('message', async (msg) => {
  // Ignora mensagens de grupo — só conversas diretas com o número do bot.
  if (msg.from.endsWith('@g.us')) {
    return;
  }

  const phone = msg.from.replace('@c.us', '');
  const body = (msg.body || '').trim();

  try {
    if (/^\d{6}$/.test(body)) {
      const reply = await callBackend('/api/internal/whatsapp/verify-code', { phone, code: body });
      await msg.reply(reply);
      return;
    }

    const lower = body.toLowerCase();
    if (CONFIRM_YES.has(lower)) {
      const reply = await callBackend('/api/internal/whatsapp/confirm', { phone, confirm: true });
      await msg.reply(reply);
      return;
    }
    if (CONFIRM_NO.has(lower)) {
      const reply = await callBackend('/api/internal/whatsapp/confirm', { phone, confirm: false });
      await msg.reply(reply);
      return;
    }

    if (msg.hasMedia) {
      const media = await msg.downloadMedia();
      if (media && media.mimetype && media.mimetype.startsWith('audio')) {
        const reply = await callBackend('/api/internal/whatsapp/message', {
          phone,
          type: 'AUDIO',
          audioBase64: media.data,
        });
        await msg.reply(reply);
      } else {
        await msg.reply('Por enquanto só entendo texto e áudio — foto ainda não. 🙂');
      }
      return;
    }

    if (body) {
      const reply = await callBackend('/api/internal/whatsapp/message', { phone, type: 'TEXT', text: body });
      await msg.reply(reply);
    }
  } catch (err) {
    console.error('Erro processando mensagem:', err);
    await msg.reply('Deu um erro por aqui processando sua mensagem. Tenta de novo.').catch(() => {});
  }
});

client.initialize();
