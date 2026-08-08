import { useEffect, useState } from 'react';
import * as profileApi from '../api/profile';
import type { Profile as ProfileData } from '../api/profile';
import * as accountsApi from '../api/accounts';
import type { Account } from '../api/accounts';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

export default function Profile() {
  const [profile, setProfile] = useState<ProfileData | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [generatedCode, setGeneratedCode] = useState<{ code: string; expiresInMinutes: number } | null>(null);
  const [requestingCode, setRequestingCode] = useState(false);

  const [savingAccount, setSavingAccount] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const [profileData, accountsData] = await Promise.all([
        profileApi.getProfile(),
        accountsApi.getAccounts(),
      ]);
      setProfile(profileData);
      setAccounts(accountsData);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar seu perfil.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleRequestCode() {
    setRequestingCode(true);
    setError('');
    try {
      const result = await profileApi.requestPhoneCode();
      setGeneratedCode(result);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível gerar o código.'));
    } finally {
      setRequestingCode(false);
    }
  }

  async function handleDefaultAccountChange(accountId: string) {
    if (!accountId) return;
    setSavingAccount(true);
    setError('');
    try {
      const updated = await profileApi.setDefaultAccount(accountId);
      setProfile(updated);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar a conta padrão.'));
    } finally {
      setSavingAccount(false);
    }
  }

  if (loading) {
    return (
      <div>
        <div className="page-header">
          <h1>Perfil</h1>
        </div>
        <div className="empty-state">Carregando...</div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1>Perfil</h1>
      </div>

      {error && <div className="auth-error">{error}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h2 style={{ marginTop: 0 }}>WhatsApp</h2>
        {profile?.phoneLinked ? (
          <p>
            Número vinculado: <strong>{profile.phoneNumber}</strong>
          </p>
        ) : (
          <>
            <p>
              Vincule seu WhatsApp pra lançar transações mandando uma mensagem (texto ou áudio) pro número
              configurado do OrçaFin.
            </p>
            {generatedCode ? (
              <div className="empty-state" style={{ textAlign: 'left' }}>
                <p>
                  Manda esse código pro número do WhatsApp do OrçaFin, dentro de{' '}
                  <strong>{generatedCode.expiresInMinutes} minutos</strong>:
                </p>
                <p style={{ fontSize: 28, fontWeight: 700, letterSpacing: 4 }}>{generatedCode.code}</p>
              </div>
            ) : (
              <button className="btn btn-primary" onClick={handleRequestCode} disabled={requestingCode}>
                {requestingCode ? 'Gerando...' : 'Vincular WhatsApp'}
              </button>
            )}
          </>
        )}
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0 }}>Conta padrão pro WhatsApp</h2>
        <p>Lançamentos criados pelo WhatsApp são salvos automaticamente nesta conta.</p>
        {accounts.length === 0 ? (
          <p>Cadastre ao menos uma conta antes de configurar isso.</p>
        ) : (
          <select
            value={profile?.defaultAccountId ?? ''}
            onChange={(e) => handleDefaultAccountChange(e.target.value)}
            disabled={savingAccount}
          >
            <option value="" disabled>
              Selecione uma conta
            </option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        )}
      </div>
    </div>
  );
}
