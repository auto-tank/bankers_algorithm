import java.util.Random;

class Constants{
    public static final int NUMBER_OF_CUSTOMERS = 5; // Total de clientes
    public static final int NUMBER_OF_RESOURCES = 3; // Total de recursos
}

public class TP1 {
    public static void main(String[] args) {
        Random random = new Random();

        if (args.length != Constants.NUMBER_OF_RESOURCES){
            System.err.println("Quantidade inicial de parâmetros de recursos incorreta");
            return;
        }

        // Válida os valores de Recursos Disponíveis passados por linha de comando
        int[] available = new int[Constants.NUMBER_OF_RESOURCES];
        try{
            for (int i=0; i < available.length; i++){
                available[i] = Integer.parseInt(args[i]);
                if (available[i] < 0) 
                    throw new NumberFormatException("Recurso Disponível não pode ser negativo.");
            }
        }
        catch (NumberFormatException e){
            System.err.println(e.toString());
            return;
        }

        // Inicia vetor de maxNeeds
        int[][] maxNeeds = new int[Constants.NUMBER_OF_CUSTOMERS][Constants.NUMBER_OF_RESOURCES]; 
       
        for(int i = 0;  i  < Constants.NUMBER_OF_CUSTOMERS; i++){
            for (int  j = 0;  j < Constants.NUMBER_OF_RESOURCES; j++){
                if(available[j] != 0){
                    maxNeeds[i][j] = random.nextInt(0,available[j]);
                }else{
                    maxNeeds[i][j] = 0;
                }
            }
        }

        for(int i = 0;  i  < Constants.NUMBER_OF_CUSTOMERS; i++){
            System.out.print("\n");
            System.out.print("| ");
            for (int  j = 0;  j < Constants.NUMBER_OF_RESOURCES; j++){
                System.out.print(maxNeeds[i][j]+" | ");
            }
        }

        Banker banker = new Banker(available, maxNeeds);
        // Espera 5 segundos antes de continuar
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.err.println("Main Thread interrompida durante a espera.");
            Thread.currentThread().interrupt(); // restaura o status de interrupção
        }

        // --- Cria e Inicia as Threads Clientes ---
        Thread[] customerHandle = new Thread[Constants.NUMBER_OF_CUSTOMERS];
        System.out.println("\nMain Thread: Criando " + Constants.NUMBER_OF_CUSTOMERS + " threads clientes...");

        for (int i = 0; i < Constants.NUMBER_OF_CUSTOMERS; i++) {
            // Cria uma instância do cliente (Runnable)
            Customer customerTask = new Customer(i, banker);
            // Cria a Thread, passando a tarefa
            customerHandle[i] = new Thread(customerTask);
            // Inicia a Thread
            customerHandle[i].start();
            System.out.println("Main Thread: Cliente " + i + " thread iniciada.");
        }

        System.out.println("\nMain Thread: Aguardando threads clientes terminarem...");

        // --- Espera pelas Threads Clientes (opcional se elas rodarem continuamente) ---
        // Se as threads rodarem em um loop contínuo, a main thread pode não esperar explicitamente por todas.
        // No entanto, se você quiser que a main thread espere (por exemplo, para uma simulação de tempo fixo), use join().
        // Para este esqueleto, vamos esperar por elas, assumindo que haveria uma condição de saída no loop do cliente em uma simulação real.
         for (int i = 0; i < Constants.NUMBER_OF_CUSTOMERS; i++) {
            try {
                customerHandle[i].join();
                System.out.println("Main Thread: Cliente " + i + " thread finalizada (joined).");
            } catch (InterruptedException e) {
                System.out.println("Main Thread interrompida enquanto esperava por clientes.");
                Thread.currentThread().interrupt();
            }
         }

        System.out.println("\nMain Thread: Todas as threads clientes terminaram (ou a espera foi concluída).");

        // Não há necessidade de limpar explicitamente Mutex/ConditionVariable em Java como em C;
        // a coleta de lixo gerencia os objetos quando não estão mais em uso.
    }
}
