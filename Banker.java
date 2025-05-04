import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;

class Banker {
    /* o montante disponível de cada recurso */
    private int available[];

    /* a demanda máxima de cada cliente */
    private int maximum[][];

    /* o montante correntemente alocado a cada cliente */
    private int allocation[][];

    /* a necessidade remanescente de cada cliente */
    private int need[][];

    // Lock para controlar o acesso às instâncias de dados compartilhadas
    private final ReentrantLock lock = new ReentrantLock();

    // Variável de condição para threads esperarem por recursos
    private final Condition resourcesAvailable = lock.newCondition();

    public Banker(int[] available, int[][] maxNeeds){
        this.available = Arrays.copyOf(available, Constants.NUMBER_OF_RESOURCES);
        this.maximum = maxNeeds; // Referenciamos o maxNeeds pois não precisaremos alterar seu valor
        // Garbage Collector garante que todos valores do 'allocation' serão 0
        this.allocation = new int[Constants.NUMBER_OF_CUSTOMERS][Constants.NUMBER_OF_RESOURCES];
        this.need = new int[Constants.NUMBER_OF_CUSTOMERS][Constants.NUMBER_OF_RESOURCES];

        

        // Inicializa a matriz need: need[i][j] = maximum[i][j] - allocation[i][j] (que nesse instante seria 0 para todos)
        for (int i = 0; i < Constants.NUMBER_OF_CUSTOMERS; i++) {
            for (int j = 0; j < Constants.NUMBER_OF_RESOURCES; j++) {
                this.need[i][j] = this.maximum[i][j] - this.allocation[i][j];
            }
        }

        System.out.println("Banqueiro inicializado.");
        printCurrentState();
    }

    // Print organizado sobre o estado atual do sistema (disponível, alocação, necessidade)
    public void printCurrentState() {
        System.out.println("\n--- Estado Atual ---");
        System.out.println("Available: " + Arrays.toString(available));

        System.out.println("Allocation:");
        for (int i = 0; i < Constants.NUMBER_OF_CUSTOMERS; i++) {
            System.out.println("  Cliente " + i + ": " + Arrays.toString(allocation[i]));
        }

        System.out.println("Need:");
        for (int i = 0; i < Constants.NUMBER_OF_CUSTOMERS; i++) {
            System.out.println("  Cliente " + i + ": " + Arrays.toString(need[i]));
        }
        System.out.println("--------------------");
    }

    public int requestResources(int customerNum, int[] request){
        /*
         * Ao utilizar 'lock()', a instância atual entra na seção crítica.
         * Ou seja, todas outras instâncias querendo acessar essa seção necessitam esperar
         * pela liberação da trava imposta por essa instância (unlock).
         * Caso essa instância também seja barrada (ex.: impossibilitada de continuar por um await),
         * a trava será liberada para que alguma outra instância lhe assuma e prossiga
         * com seus comandos até ser sinalizada sua liberação.
         
         * A liberação acontece por meio do 'signalAll()' e, quando feita, sinaliza para que
         * todas instâncias que estiverem esperando pela liberação da trava 'apostem corrida' 
         * para assumir 'ownership' da trava, assim seguindo adiante com seus comandos.
        */
        lock.lock();

        try{
            System.out.println("Cliente " + customerNum + " solicitando: " + Arrays.toString(request));

            // --- Algoritmo do Banqueiro (REQUISIÇÃO) ---

            // ! Passo 1: Verificar se a Request > Need (Solicitação <= Necessidade)
            // (requisição pede por mais do que há de recursos necessários (máximo))
            for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                if (request[i] > need[customerNum][i]) {
                    System.out.println("Cliente " + customerNum + ": Erro! Requisição excede a necessidade máxima.");
                    return -1; 
                }
            }
            
            // Lógica de loop para caso não possa ser atendido imediatamente por falta de recursos
            // ou por resultar em estado inseguro (falha da condicional isSafe)
            while (true) { 
                // ! Passo 2: Verificar se a Request > Available (Solicitação <= Disponível)
                // (requisição pede por mais do que há de recursos disponíveis)
                boolean canGrantResources = true;
                for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                    if (request[i] > available[i]) {
                        canGrantResources = false;
                        break;
                    }
                }

                // Se os recursos não estiverem disponíveis, 
                // ativa a condição de espera para esse cliente pelos recursos requisitados
                if (!canGrantResources) {
                    System.out.println("Cliente " + customerNum + ": Recursos insuficientes disponíveis. Esperando...");
                    try {
                        /* 
                         * Essa parte é aonde acontece a espera pela trava, servindo como uma 'barreira'
                         * como mencionado anteriormente.
                         * Após receber um sinal de liberação (signalAll), acorda e entra em uma corrida contra outras
                         * instâncias para competir pela 'ownership' dessa trava.
                         
                         * Se houver sucesso na corrida, após o 'unlock()', essa instância se tornará a qual
                         * vai continuar a rodar seus comandos a partir deste ponto.
                        */
                        resourcesAvailable.await();
                    } catch (InterruptedException e) {
                        System.out.println("Cliente " + customerNum + " interrompido enquanto esperava.");
                        Thread.currentThread().interrupt();
                        return -1; 
                    }
                    // Caso tenha ganhado a corrida, isso não quer dizer que os recursos foram propriamente liberados
                    // (o destravamento da lock pode ter sido por conta de outra requisição de recursos)
                    continue; // Então volta do inicio para realmente validar se há recursos disponíveis.
                }

                // ! Passo 3: Se Request <= Available, simular a alocação e verificar se o estado é seguro

                // Cópias temporárias para simulação de se está seguro fazer a atribuição
                int[] tempAvailable = Arrays.copyOf(available, Constants.NUMBER_OF_RESOURCES);

                int[][] tempAllocation = new int[Constants.NUMBER_OF_CUSTOMERS][Constants.NUMBER_OF_RESOURCES];
                for(int i=0; i < Constants.NUMBER_OF_CUSTOMERS; i++) 
                    tempAllocation[i] = Arrays.copyOf(allocation[i], Constants.NUMBER_OF_RESOURCES);

                int[][] tempNeed = new int[Constants.NUMBER_OF_CUSTOMERS][Constants.NUMBER_OF_RESOURCES];
                for(int i=0; i < Constants.NUMBER_OF_CUSTOMERS; i++) 
                    tempNeed[i] = Arrays.copyOf(need[i], Constants.NUMBER_OF_RESOURCES);

                // Simular a atribuição temporária
                for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                    /* (Temporária, mas se aplica para a final)
                     * Lógica de Alocação:
                     * - Os recursos requisitados são "retirados" do disponível;
                     * - Os recursos requisitados são "adicionados" à alocação do cliente (em uso);
                     * - A necessidade restante do cliente é "reduzida".
                    */
                    tempAvailable[i] -= request[i];
                    tempAllocation[customerNum][i] += request[i];
                    tempNeed[customerNum][i] -= request[i];
                }

                // Fazemos o teste de segurança com os dados simulados pois não destruiremos a lógica principal
                if (isSafe(tempAvailable, tempAllocation, tempNeed)){
                    // Pemitir a requisição do cliente se realmente for seguro (aplicar as mudanças ao estado real)
                    available = tempAvailable;
                    allocation = tempAllocation;
                    need = tempNeed;

                    System.out.println("Cliente " + customerNum + ": Requisição concedida. Agora disponíveis: " + Arrays.toString(available));
                    return 0;
                } 
                else {
                    System.out.println("Cliente " + customerNum + ": Requisição negada (levaria a estado inseguro). Esperando...");
                    try {
                        // Novamente entra em estado de espera para a possível liberação de novos recursos  
                        resourcesAvailable.await();
                    } catch (InterruptedException e) {
                        System.out.println("Cliente " + customerNum + " interrompido enquanto esperava (estado inseguro).");
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                    // Após receber a trava (acordar), o loop while fará a thread verificar a requisição novamente
                }
            }
        } finally {
            lock.unlock(); // Destrava mesmo que hajam erros, assim a próxima requisição sempre vai rodar
        }
    }

    private boolean isSafe(int[] currentAvailable, int[][] currentAllocation, int[][] currentNeed){
        // --- Algoritmo do Banqueiro (REQUISIÇÃO: Checagem de Segurança) ---

        // ! Passo 1: Trabalho = Disponível e Término[i] = false
        int[] work = Arrays.copyOf(currentAvailable, Constants.NUMBER_OF_RESOURCES);
        boolean[] finish = new boolean[Constants.NUMBER_OF_CUSTOMERS];
        for(int i=0; i < Constants.NUMBER_OF_CUSTOMERS; i++)
            finish[i] = false;

        boolean validationFinished;
        do {
            validationFinished = false; // A ser explicado no final quando seu valor é alterado

            // ! Passo 2: Término[i] == false E Necessidade[i] <= Trabalho
            for(int i=0; i < Constants.NUMBER_OF_CUSTOMERS; i++){
    
                // Término[i] == false
                // ou seja, todos que já tiverem sido validados (true) não precisam ser validados novamente
                if (!finish[i]){
    
                    // Verifica se Necessidade[i] <= Trabalho para todas posições de Necessidade
                    boolean validateWork = true;
                    for (int j = 0; j < Constants.NUMBER_OF_RESOURCES; j++) {
                        /*
                         * A Necessidade acaba sendo maior que o Trabalho disponível,
                         * logo não seria possível validar seu pedido.
                        */
                        if (currentNeed[i][j] > work[j]) {
                            validateWork = false;
                            break; // Se falhar em uma, todo trabalho posterior ou anterior ficaria inválido da mesma forma
                        }
                    }
    
                    // Necessidade[i] <= Trabalho
                    if (validateWork){
                        // ! Passo 3: Trabalho = Trabalho + Alocação e Término[i] = true
    
                        /*
                         * Essa parte seria uma simulação de liberação de recursos pelo cliente,
                         * logo a soma de Trabalho com Alocação resultaria na devolução dos recursos
                         * previamente tomados pelo requerente (cliente).
                        */
                        for (int j=0; j < Constants.NUMBER_OF_RESOURCES; j++){
                            work[j] += currentAllocation[i][j];
                        }
                        finish[i] = true; // Término[i] = true

                        /*
                         * Após um cliente[i] liberar seus recursos, isso possibilita que a lógica de
                         * segurança seja rodada novamente (Retornar ao Passo 2).
                         * Isso impõe que, agora com mais Trabalho disponível, outro cliente[i] possa
                         * usufruir propriamente do Trabalho e validar se suas transações (Necessidade <= Trabalho)
                         * sejam realmente válidas.
                        
                         * Essa "re-validação" acontece dentro do loop do-while pois necessitamos validar
                         * para os cliente[i] que são anteriores a este, logo precisando rodar
                         * toda lógica novamente (a partir do primeiro cliente[i]).
                        */
                        validationFinished = true;
                    }
                }
            }
        } while (validationFinished);

        // ! Passo 4: Término[i] == true para todo i, sistema é seguro
        int counter = 0;
        for (int i=0; i < finish.length; i++){
            if (finish[i]) counter++; // Se todos os casos de Término[i] retornar verdadeiro
        }
        if (counter == finish.length) return true;

        return false;
    }

    public int releaseResources(int customerNum, int[] release){
        // Adquire a trava para controle de acesso (Explicada no requestResources)
        lock.lock();

        try {
            System.out.println("Cliente " + customerNum + " liberando: " + Arrays.toString(release));

            // --- Algoritmo do Banqueiro (LIBERAÇÃO) ---

            // ! Passo 1: Verificar se a Release > Allocation (Liberação <= Alocação)
            // (liberação pede por mais do que há de recursos alocados (em uso))
            for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                if (release[i] > allocation[customerNum][i]) {
                    System.out.println("Cliente " + customerNum + ": Erro! Tentando liberar mais recursos do que alocado.");
                    return -1; 
                }
            }

            // ! Passo 2: Aplicar a liberação (atualizar available, allocation, need)
            for (int i = 0; i < Constants.NUMBER_OF_RESOURCES; i++) {
                /*
                    * Lógica de Liberação:
                    * - Os recursos liberados são "adicionados" ao disponível;
                    * - Os recursos liberados são "retirados" da alocação do cliente (não mais em uso);
                    * - A necessidade restante do cliente é "aumentada".
                */
                available[i] += release[i];
                allocation[customerNum][i] -= release[i];
                need[customerNum][i] += release[i];
            }

            System.out.println("Cliente " + customerNum + ": Recursos liberados. Agora disponíveis: " + Arrays.toString(available));

            /*
             * Aqui notificamos todas as threads esperando na condição de lock() ou de await()
             * para acordarem e esperar na fila de liberação da lock.
             * Assim, a instância que ganhar a corrida pela lock (primeiro da fila), irá assumir 
             * suas atividades enquanto as outras permanecerão em um estado ocioso por 
             * outra oportunidade de liberação. 
            */
            resourcesAvailable.signalAll();
            return 0;
        } finally {
            lock.unlock(); // Destrava mesmo que hajam erros, assim a próxima requisição sempre vai rodar
        }
    }

    public int getNeedValue(int customerNum, int valuePos){
        return this.need[customerNum][valuePos];
    }

    public int[] getAllocationRow(int customerNum){
        return this.allocation[customerNum];
    }
}
