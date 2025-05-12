package model;

/**
 * Representa o usuário carregado do banco, com as propriedades
 * necessárias para as várias fases de autenticação.
 */
public class User {
    private int     uid;
    private String  nome;
    private String  email;
    private String  passwordHash;
    private byte[] encryptedTotp;    // novo
    private String  totpSecret;        // ← permanece para, depois, armazenar o Base32
    private boolean frozen;
    private int     chaveiroId;

    /** Novo construtor completo */
    public User(int uid,
                String nome,
                String email,
                String passwordHash,
                byte[] encryptedTotpKey,
                boolean frozen) {
        this.uid               = uid;
        this.nome              = nome;
        this.email             = email;
        this.passwordHash      = passwordHash;
        this.encryptedTotp  = encryptedTotpKey;
        this.frozen            = frozen;
    }

    /** Construtor de compatibilidade (usado hoje pelo DBManager) */
    public User(String email,
                String passwordHash,
                String totpSecret,
                boolean frozen) {
        this(0, null, email, passwordHash, null, frozen);
        this.totpSecret = totpSecret;
    }

    
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }

    public String getPasswordHash() { return passwordHash; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public byte[] getEncryptedTotp() { return encryptedTotp; }
    public void setEncryptedTotp(byte[] encryptedTotp) { this.encryptedTotp = encryptedTotp; }


    public boolean isFrozen() { return frozen; }

    public int getChaveiroId() { return chaveiroId; }
    public void setChaveiroId(int chaveiroId) { this.chaveiroId = chaveiroId; }
}
