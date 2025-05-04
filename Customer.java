import java.util.concurrent.ThreadLocalRandom;

class Customer implements Runnable {
    private int customerId;
    private Banker banker;
    private ThreadLocalRandom random = ThreadLocalRandom.current(); // Gerador aleatório seguro para threads

    public Customer(int id, Banker banker) {
        this.customerId = id;
        this.banker = banker;
    }

    //? Codigo que será executado pelo Customer(thread) continuamente 
    @Override
    public void run() {
        System.out.println("Cliente " + customerId + " thread iniciada.");

        while (true) {
            //! --- Simular Requisição de Recursos ---
            int[] request = new int[Constants.NUMBER_OF_RESOURCES];
            boolean madeRequest = false;
            //* Gera uma requisição aleatória com o maximo sendo a necessidade do Customer
            for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                // Getter seguro para descobrir o need atual do Customer
                int currentNeed = banker.getNeedValue(customerId, i);
                if (currentNeed > 0) {
                    // Gera entre 0 e currentNeed (inclusive)
                    request[i] = random.nextInt(currentNeed + 1); 
                    if (request[i] > 0)
                        madeRequest = true;
                } else {
                    request[i] = 0;
                }
            }

            if (madeRequest) {
                // Chamada ao metodo para requisitar recursos do Banqueiro(banker)
                int result = banker.requestResources(customerId, request);

                if (result == 0) { // Se tivermos retorno 0 a requisição foi um sucesso
                    // Simular uso dos recursos por um tempo 50-250 ms
                    System.out.println("Cliente " + customerId + ": Usando recursos...");
                    try {
                        Thread.sleep(random.nextInt(50) + 200);
                    } catch (InterruptedException e) {
                        System.out.println("Cliente " + customerId + " interrompido enquanto usava recursos.");
                        Thread.currentThread().interrupt();
                        return; // Termina a thread se for interrompida durante o sleep
                    }

                    //! --- Simular Liberação de Recursos ---
                    int[] release = new int[Constants.NUMBER_OF_RESOURCES];
                    //Variavel que verifica se a liberação é valida
                    boolean madeRelease = false;
                    // Getter para saber quantos recursos está alocado (em uso) ao Customer
                    int[] currentAllocation = banker.getAllocationRow(customerId); 

                    for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                        if (currentAllocation[i] > 0) {
                            // Cria vetor de liberação de recuros com valores entre 0 e alocado(inclusive)
                            release[i] = random.nextInt(currentAllocation[i]) + 1; 
                            if (release[i] > 0)
                                //Liberação é valida se pelo menos um recurso for ser liberado
                                madeRelease = true;
                        } else {
                            release[i] = 0;
                        }
                    }

                    if (madeRelease) {
                        // Chamar o método de liberação de recursos do banqueiro
                        banker.releaseResources(customerId, release);
                    }

                } // se result == -1, a thread esperou e o loop vai tentar novamente
                
                
                /*
                 * Se o Customer tiver requisitado toda sua necessicade e não tiver liberado nada 
                 * ele ficara sem necessidade de recursos e isso poderia causar um erro com ele prendendo os recursos. 
                 * Para evitar isso se o Custormer estiver sem necessidade de recursos ao fim de um release
                 * soltamos todos os recursos que estão com ele
                */
                boolean noNeeds = true;

                for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                    if (banker.getNeedValue(customerId, i) > 0) {
                        noNeeds = false;
                        break;
                    }
                }

                if (noNeeds) {
                    banker.releaseResources(customerId, banker.getAllocationRow(customerId));
                    System.out.println("Cliente " + customerId + " utilizou todas os recursos necessários!!!.");
                }

            } else { // Se não fez requisição (random foi 0)
                try {
                    Thread.sleep(50); // Pequena pausa
                } catch (InterruptedException e) {
                    System.out.println("Cliente " + customerId + " interrompido em pausa.");
                    Thread.currentThread().interrupt();
                    return;
                }
            }

           
        } // Fim do loop while(true)
    }
}