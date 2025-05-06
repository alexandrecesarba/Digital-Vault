package model;

/**
 * Representa o usuário carregado do banco, com as propriedades
 * necessárias para autenticação.
 */

public class User {
    private final int    uid;          // novo
    private final String email;
    private final String passwordHash;
    private final String totpSecret;
    private final boolean frozen;

    /**
     * @param uid           ID interno do usuário (campo uid em Usuarios)
     * @param email         e-mail (login name)
     * @param passwordHash  hash Bcrypt da senha pessoal
     * @param totpSecret    chave secreta TOTP em Base32
     * @param frozen        true se o usuário estiver bloqueado
     */
    public User(int uid,
                String email,
                String passwordHash,
                String totpSecret,
                boolean frozen) {
        this.uid          = uid;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.totpSecret   = totpSecret;
        this.frozen       = frozen;
    }

    public int getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public boolean isFrozen() {
        return frozen;
    }
}
