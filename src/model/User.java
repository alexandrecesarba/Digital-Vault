package model;

/**
 * Representa o usuário carregado do banco, com as propriedades
 * necessárias para autenticação.
 */
public class User {
    private int     uid;           // já tem
    private String  nome;          // novo
    private String  email;
    private String  passwordHash;
    private String  totpSecret;
    private boolean frozen;
    private int     chaveiroId;    // novo

    /**
     * @param uid           ID interno do usuário (campo uid em Usuarios)
     * @param nome          nome completo do usuário
     * @param email         e-mail (login name)
     * @param passwordHash  hash Bcrypt da senha pessoal
     * @param totpSecret    chave secreta TOTP em Base32
     * @param frozen        true se o usuário estiver bloqueado
     */
    public User(int uid,
                String nome,
                String email,
                String passwordHash,
                String totpSecret,
                boolean frozen) {
        this.uid           = uid;
        this.nome          = nome;
        this.email         = email;
        this.passwordHash  = passwordHash;
        this.totpSecret    = totpSecret;
        this.frozen        = frozen;
    }

        /**
     * Este é o _constructor_ de compatibilidade que o DBManager
     * ainda usa, para não ter que alterar todo mundo.
     */
    public User(String email,
                String passwordHash,
                String totpSecret,
                boolean frozen) {
        // passamos valores “placeholder” para os campos que só
        // serão preenchidos depois, se necessário.
        this(0, null, email, passwordHash, totpSecret, frozen);
    }

    // --- UID ---
    public int getUid() {
        return uid;
    }
    public void setUid(int uid) {
        this.uid = uid;
    }

    // --- Nome ---
    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    // --- Email ---
    public String getEmail() {
        return email;
    }

    // --- Password hash ---
    public String getPasswordHash() {
        return passwordHash;
    }

    // --- TOTP secret ---
    public String getTotpSecret() {
        return totpSecret;
    }

    // --- Frozen/block flag ---
    public boolean isFrozen() {
        return frozen;
    }

    // --- Chaveiro ID ---
    public int getChaveiroId() {
        return chaveiroId;
    }
    public void setChaveiroId(int chaveiroId) {
        this.chaveiroId = chaveiroId;
    }
}
