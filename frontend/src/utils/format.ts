export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value ?? 0);
}

// Aceita tanto "85,64" (padrão brasileiro, com ou sem separador de milhar "1.234,56")
// quanto "85.64" (já vem assim quando pré-preenchido pela IA).
export function parseDecimal(value: string): number {
  const trimmed = value.trim();
  const normalized = trimmed.includes(',') ? trimmed.replace(/\./g, '').replace(',', '.') : trimmed;
  const parsed = parseFloat(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  if (!y || !m || !d) return dateStr;
  return `${d}/${m}/${y}`;
}

export function formatDateTime(isoStr: string): string {
  if (!isoStr) return '';
  const date = new Date(isoStr);
  if (Number.isNaN(date.getTime())) return isoStr;
  return date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export function todayISO(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function monthRange(year: number, month: number): { from: string; to: string } {
  const from = `${year}-${String(month).padStart(2, '0')}-01`;
  const lastDay = new Date(year, month, 0).getDate();
  const to = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
  return { from, to };
}

export const accountTypeLabels: Record<string, string> = {
  CORRENTE: 'Conta Corrente',
  POUPANCA: 'Poupança',
  CARTEIRA: 'Carteira',
};

export const paymentMethodLabels: Record<string, string> = {
  DINHEIRO: 'Dinheiro',
  DEBITO: 'Débito',
  CREDITO: 'Crédito',
  PIX: 'Pix',
  TED: 'TED',
  DOC: 'DOC',
  TRANSFERENCIA: 'Transferência',
};

export const transactionGroupLabels: Record<string, string> = {
  ASSINATURAS: 'Assinaturas',
  CONTAS_FIXAS_CASA: 'Contas Fixas da Casa',
  COMPRAS_PONTUAIS: 'Compras Pontuais',
  DIVIDAS_PARCELAS: 'Dívidas / Parcelas',
  ALIMENTACAO_CASA: 'Alimentação de Casa',
  RESTAURANTES: 'Restaurantes',
  FATURA_CARTAO: 'Fatura de Cartão',
  VALE_TRANSPORTE: 'Vale-Transporte',
  VALE_ALIMENTACAO_REFEICAO: 'Vale-Alimentação / Refeição',
  INVESTIMENTOS: 'Investimentos',
  CAIXINHAS: 'Caixinhas',
  AJUSTES_ESTORNOS: 'Ajustes e Estornos',
  TARIFAS_JUROS: 'Tarifas e Juros',
  OUTROS: 'Outros',
};

export const prepaidCardTypeLabels: Record<string, string> = {
  VALE_ALIMENTACAO: 'Vale Alimentação',
  VALE_REFEICAO: 'Vale Refeição',
  USO_LIVRE: 'Uso Livre',
  MOBILIDADE: 'Mobilidade / Combustível',
  VALE_TRANSPORTE: 'Vale-Transporte',
};

export const transitSubtypeLabels: Record<string, string> = {
  URBANO: 'Urbano',
  INTERMUNICIPAL: 'Intermunicipal',
  METRO: 'Metrô',
  ESTUDANTE_MEIA_PASSAGEM: 'Estudante (meia-passagem)',
};

export const investmentTypeLabels: Record<string, string> = {
  RENDA_FIXA: 'Renda Fixa',
  TESOURO_DIRETO: 'Tesouro Direto',
  ACOES: 'Ações',
  FUNDOS_IMOBILIARIOS: 'Fundos Imobiliários',
  CRIPTOMOEDAS: 'Criptomoedas',
  FUNDOS: 'Fundos',
  OUTROS: 'Outros',
};

export const vaultMovementTypeLabels: Record<string, string> = {
  APORTE: 'Aporte',
  RESGATE: 'Resgate',
  RENDIMENTO: 'Rendimento',
};

export const transactionTypeLabels: Record<string, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
  TRANSFERENCIA: 'Transferência',
};

export const recurrenceFrequencyLabels: Record<string, string> = {
  DIARIA: 'Diária',
  SEMANAL: 'Semanal',
  MENSAL: 'Mensal',
  ANUAL: 'Anual',
};
