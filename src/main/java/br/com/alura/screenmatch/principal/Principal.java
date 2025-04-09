package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.DadosEpisodio;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.*;


public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    public void exibeMenu() {
        System.out.println("Digite o nome da serie para busca :");
        var nomeSerie = leitura.nextLine();

        try {
            // Codificando o nome da série para garantir que espaços e caracteres especiais sejam tratados
            String nomeSerieCodificado = URLEncoder.encode(nomeSerie, StandardCharsets.UTF_8.toString());

            // Obtendo os dados da série
            var json = consumo.obterDados(ENDERECO + nomeSerieCodificado + API_KEY);
            DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
            System.out.println(dados);

            // Lista para armazenar as temporadas
            List<DadosTemporada> temporadas = new ArrayList<>();

            // Obtendo os dados de cada temporada
            for (int i = 1; i <= dados.totalTemporadas(); i++) {
                json = consumo.obterDados(ENDERECO + nomeSerieCodificado + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }

            // Exibindo informações das temporadas e episódios
            temporadas.forEach(System.out::println);
            temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));

            // Listando todos os episódios
            List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream())
                    .collect(Collectors.toList());

            // Exibindo os 5 episódios com a maior avaliação
            dadosEpisodios.stream()
                    .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                    .peek( e -> System.out.println("Priemiro Filtro N/A" + e))
                    .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                    .peek(e -> System.out.println("Ordenação" + e))
                    .limit(10)
                    .peek(e -> System.out.println("Limite" + e))
                    .map(e -> e.titulo().toUpperCase())
                    .peek(e -> System.out.println("Mapeamento" + e))
                    .forEach(System.out::println);

            // Listando os episódios com informações de temporada
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream()
                            .map(d -> new Episodio(t.numeroTemporada(), d))
                    ).collect(Collectors.toList());

            episodios.forEach(System.out::println);

            System.out.println("Digite um trecho do titulo do episodio:");

            var trechoTitulo = leitura.nextLine();
            Optional<Episodio> episodioBuscado = episodios.stream()
                    .filter(e -> e.getTitulo().toUpperCase().contains(trechoTitulo.toUpperCase()))
                    .findFirst();
            if (episodioBuscado.isPresent()){
                System.out.println("Episodio encontrado");
                System.out.println("Temporada: " + episodioBuscado.get().getTemporada());
            }else {
                System.out.println("Episodio não encontrado!");
            }

            //  Filtrar episódios lançados após essa data
            System.out.println("A partir de que ano você deseja ver os episodios?:");
            var ano = leitura.nextInt();
            leitura.nextLine();

            // Definindo a data mínima para exibição
            LocalDate dataBusca = LocalDate.of(ano, 1, 1);
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Exibindo episódios lançados após o ano fornecido
            episodios.stream()
                    .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca))
                    .forEach(e -> System.out.println(
                            "Temporada: " + e.getTemporada() +
                                    " Episódio: " + e.getTitulo() +
                                    " Data lançamento: " + e.getDataLancamento().format(formatador)
                    ));

            Map<Integer, Double> avaliacoesPorTemporada = episodioBuscado.stream()
                    .filter(e -> e.getAvaliacao() > 0.0)
                    .collect(Collectors.groupingBy(Episodio::getTemporada, Collectors.averagingDouble(Episodio::getAvaliacao)));
            System.out.println(avaliacoesPorTemporada);

            DoubleSummaryStatistics est = episodios.stream()
                    .filter(e -> e.getAvaliacao() > 0.0)
                    .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));
            System.out.println("Média: " + est.getAverage());
            System.out.println("Melhor episódio: " + est.getMax());
            System.out.println("Pior episódio: " + est.getMin());
            System.out.println("Quantidade: " + est.getCount());


        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
