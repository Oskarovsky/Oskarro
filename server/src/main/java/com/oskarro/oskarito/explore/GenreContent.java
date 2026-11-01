package com.oskarro.oskarito.explore;

import java.util.List;
import java.util.Map;

/**
 * Long-form text and FAQ for each genre page. Shown to visitors under the track list and
 * rendered by the server for search engines (FAQPage structured data).
 */
public final class GenreContent {

    public record Faq(String q, String a) {
    }

    public record Content(List<String> intro, List<Faq> faq) {
    }

    private GenreContent() {
    }

    public static Content of(String slug) {
        return CONTENT.get(slug);
    }

    private static final Map<String, Content> CONTENT = Map.ofEntries(
            Map.entry("club", new Content(List.of(
                    "Muzyka klubowa to szeroki worek: od tanecznego house’u, przez big room i electro, po remiksy radiowych przebojów robione pod parkiet. Łączy ją jedno — bit w tempie mniej więcej 120–130 BPM i budowa numeru, która pozwala didżejowi płynnie przechodzić z utworu w utwór.",
                    "Na VIXA zbieramy nowości club dodawane przez społeczność: extended mixy, bootlegi i remiksy, które grają polskie kluby i domówki. Każdy numer możesz odsłuchać za darmo, ocenić i dodać do swojej składanki — do auta, na siłownię albo na imprezę. Najlepsze utwory trafiają do rankingu tygodnia i miesiąca."),
                    List.of(new Faq("Co to jest muzyka klubowa?", "To muzyka taneczna tworzona z myślą o klubach i imprezach: house, electro, big room, dance i remiksy popularnych utworów, zwykle w tempie 120–130 BPM."),
                            new Faq("Gdzie posłuchać nowości klubowych 2026?", "Na VIXA w zakładce Club — utwory są ułożone od najnowszych, a co miesiąc powstaje strona „Nowości club” z numerami dodanymi w danym miesiącu."),
                            new Faq("Czym różni się extended mix od radio edit?", "Extended mix ma dłuższe wejście i wyjście bez wokalu, żeby didżej mógł go zmiksować z innym numerem. Radio edit jest krótszy i od razu przechodzi do refrenu.")))),
            Map.entry("dance", new Content(List.of(
                    "Dance to melodyjna, piosenkowa strona muzyki tanecznej: wyraźny refren, wokal, który łatwo zaśpiewać, i energia, która działa i w radiu, i na weselu. Tempo zwykle mieści się między 118 a 128 BPM.",
                    "Tutaj znajdziesz single i remiksy dance dodawane przez ludzi z VIXA — nowe hity i przypomniane klasyki. Słuchasz za darmo, możesz ocenić numer i zapisać go w składance do auta."),
                    List.of(new Faq("Czym dance różni się od muzyki klubowej?", "Dance stawia na piosenkę i refren, muzyka klubowa częściej na bit i budowanie napięcia pod parkiet. W praktyce oba style często się przenikają."),
                            new Faq("Jaka muzyka dance na imprezę?", "Najlepiej sprawdzają się numery z mocnym refrenem — zobacz ranking dance tygodnia na VIXA, ułożony według głosów i odsłuchów."),
                            new Faq("Czy mogę dodać swój remiks dance?", "Tak. Załóż konto, kliknij „Dodaj utwór”, wybierz gatunek Dance i wklej link do pliku z soundfiles.eu.")))),
            Map.entry("retro", new Content(List.of(
                    "Retro to klubowe i taneczne hity, przy których bawiło się poprzednie pokolenie — eurodance z lat 90., klasyki z dyskotek i stare klubowe bangery, często w nowych remiksach.",
                    "Na VIXA zbieramy retro numery i odświeżone wersje znanych przebojów. To dobry wybór na imprezę 30+, wesele albo trasę samochodem, kiedy chcesz, żeby wszyscy znali słowa."),
                    List.of(new Faq("Jaka muzyka retro na imprezę?", "Eurodance z lat 90., klasyki dyskotekowe i ich nowe remiksy — sprawdź ranking retro na VIXA."),
                            new Faq("Co to jest „old school” w muzyce klubowej?", "Tak potocznie nazywa się klubowe numery sprzed lat, które wracają w setach didżejów i remiksach."),
                            new Faq("Czy są tu remiksy starych hitów?", "Tak — wiele utworów w retro to nowe wersje znanych przebojów, przygotowane pod dzisiejsze parkiety.")))),
            Map.entry("house", new Content(List.of(
                    "House narodził się w klubach Chicago w latach 80. i do dziś jest fundamentem muzyki klubowej: równa stopa na każdą ćwierćnutę, ciepły bas i groove, przy którym można tańczyć godzinami. Tempo to zwykle 120–126 BPM.",
                    "Na VIXA znajdziesz deep house, tech house, funky i vocal house — od spokojnych numerów na wieczór po energetyczne kawałki na peak time. Słuchaj za darmo i układaj własne składanki."),
                    List.of(new Faq("Co to jest muzyka house?", "Gatunek muzyki klubowej z Chicago lat 80., oparty na równym bicie 4/4, basie i powtarzalnym groovie."),
                            new Faq("Jakie są odmiany house?", "Między innymi deep house, tech house, progressive house, bass house i vocal house."),
                            new Faq("Jakie tempo ma house?", "Najczęściej 120–126 uderzeń na minutę (BPM).")))),
            Map.entry("vixa", new Content(List.of(
                    "Vixa (wixa) to polska odpowiedź na pytanie, jak rozkręcić imprezę: szybkie, pompujące bity, mocne basy i chwytliwe, często przerabiane wokale. Wyrosła z domówek, osiedlowych imprez i muzyki „do auta”, a dziś ma własne sety, didżejów i wierną publiczność.",
                    "VIXA to miejsce dla vixy — nowe pompy, bootlegi i remiksy dodawane przez społeczność, ranking tygodnia i miesiąca oraz radio, które gra bez przerwy. Słuchasz za darmo, oceniasz numery i budujesz własną składankę do auta."),
                    List.of(new Faq("Co to jest vixa?", "Polski styl muzyki imprezowej: szybkie, mocne bity, pompujący bas i przerabiane wokale. Kojarzy się z domówkami i muzyką do auta."),
                            new Faq("Jak się pisze: vixa czy wixa?", "Spotkasz obie formy — „vixa” jest dziś popularniejsza w tytułach setów i playlist, „wixa” to wcześniejsza pisownia."),
                            new Faq("Gdzie znaleźć vixę do auta 2026?", "Na VIXA w zakładce Vixa oraz na stronach „Nowości vixa” z numerami dodanymi w każdym miesiącu.")))),
            Map.entry("bass-house", new Content(List.of(
                    "Bass house łączy klubowy groove house’u z ciężkim, przesterowanym basem i agresywnymi dropami. To muzyka, która pcha do przodu — idealna na siłownię, do auta i na późną godzinę na parkiecie.",
                    "Na VIXA znajdziesz nowości bass house i remiksy dodawane przez społeczność. Ułóż z nich składankę na trening albo sprawdź, co gra w rankingu tygodnia."),
                    List.of(new Faq("Co to jest bass house?", "Odmiana house z bardzo mocnym, przesterowanym basem i wyraźnymi dropami, zwykle w tempie 124–128 BPM."),
                            new Faq("Bass house na siłownię — co wybrać?", "Numery z najmocniejszym dropem — sprawdź ranking bass house na VIXA."),
                            new Faq("Czym różni się bass house od tech house?", "Tech house jest bardziej minimalny i rytmiczny, bass house stawia na ciężki, „brudny” bas.")))),
            Map.entry("techno", new Content(List.of(
                    "Techno to hipnotyczna, powtarzalna muzyka, która powstała w Detroit i rozkwitła w Berlinie. Liczy się w nim rytm, przestrzeń i stopniowe budowanie napięcia — od melodic techno po twardy, industrialny bit.",
                    "Na VIXA zbieramy techno na nocną jazdę, trening i długie sety. Słuchaj za darmo, oceniaj i zapisuj ulubione numery w swoich składankach."),
                    List.of(new Faq("Co to jest techno?", "Muzyka elektroniczna z Detroit lat 80., oparta na powtarzalnym rytmie, syntezatorach i stopniowo budowanym napięciu, zwykle 125–140 BPM."),
                            new Faq("Czym różni się techno od house?", "House jest cieplejszy i bardziej „piosenkowy”, techno — surowsze, mroczniejsze i bardziej hipnotyczne."),
                            new Faq("Jakie są odmiany techno?", "Między innymi melodic techno, peak time, hard techno, industrial i minimal.")))),
            Map.entry("big-room", new Content(List.of(
                    "Big room to festiwalowe brzmienie: proste, potężne dropy, krótkie melodie do skakania i budowanie napięcia pod ręce w górze. Złote lata przeżywał na największych scenach EDM, a dziś wraca w nowych produkcjach i mashupach.",
                    "Na VIXA znajdziesz big room i festiwalowe mashupy dodawane przez społeczność — na domówkę, do auta i na parkiet."),
                    List.of(new Faq("Co to jest big room?", "Festiwalowa odmiana muzyki elektronicznej z prostymi, bardzo mocnymi dropami, zwykle 126–130 BPM."),
                            new Faq("Big room na imprezę — działa?", "Tak, zwłaszcza na peak time, gdy parkiet jest pełny — sprawdź ranking big room na VIXA."),
                            new Faq("Co to jest mashup?", "Połączenie dwóch lub więcej utworów w jeden — popularne w big roomie i setach festiwalowych.")))),
            Map.entry("trance", new Content(List.of(
                    "Trance to długie budowanie, euforyczne melodie i breakdowny, w których cała sala na chwilę się zatrzymuje. Od klasyki z przełomu wieków po współczesny uplifting i progressive trance — ten gatunek ma jedną z najwierniejszych publiczności.",
                    "Na VIXA zbieramy trance na trening, drogę i noc. Słuchaj za darmo, oceniaj numery i dodawaj je do składanek."),
                    List.of(new Faq("Co to jest trance?", "Gatunek muzyki elektronicznej z lat 90., oparty na melodiach, długim budowaniu napięcia i euforycznych breakdownach, zwykle 130–140 BPM."),
                            new Faq("Jakie są odmiany trance?", "Między innymi uplifting, progressive, vocal, psytrance i tech trance."),
                            new Faq("Trance do biegania — dlaczego?", "Stałe, szybkie tempo i budowanie emocji dobrze trzymają rytm na treningu i w długiej trasie.")))),
            Map.entry("electro", new Content(List.of(
                    "Electro to klubowa energia z wyraźnym, syntetycznym brzmieniem: ostre leady, mocny bas i rytm, który podkręca tempo imprezy. Mieści się tu electro house, complextro i ich współczesne odmiany.",
                    "Na VIXA znajdziesz electro dodawane przez społeczność — sprawdź nowości, ranking i ułóż składankę na imprezę."),
                    List.of(new Faq("Co to jest electro house?", "Odmiana house z ostrzejszym, syntetycznym brzmieniem i mocnym basem, popularna w klubach i na festiwalach."),
                            new Faq("Electro czy big room na imprezę?", "Electro daje więcej brudnego, rytmicznego brzmienia, big room — prostsze, festiwalowe dropy. Najlepiej przeplatać oba."),
                            new Faq("Gdzie znaleźć nowości electro?", "Na VIXA w zakładce Electro i na miesięcznych stronach z nowościami.")))),
            Map.entry("hands-up", new Content(List.of(
                    "Hands up to szybka, melodyjna muzyka taneczna z niemieckich i polskich dyskotek: wysokie tempo, chwytliwe leady i refreny, przy których cała sala podnosi ręce. Często sięga po przeróbki znanych przebojów.",
                    "Na VIXA zbieramy hands up — od klasyki po nowe produkcje. Idealne na imprezę, do auta i na wieczór, kiedy chcesz, żeby było szybko i melodyjnie."),
                    List.of(new Faq("Co to jest hands up?", "Szybka (zwykle 140–150 BPM), melodyjna muzyka taneczna z mocnym leadem i refrenami, popularna w Niemczech i Polsce."),
                            new Faq("Czym hands up różni się od trance?", "Hands up jest prostszy, bardziej piosenkowy i „dyskotekowy”, trance stawia na długie budowanie i atmosferę."),
                            new Faq("Czy hands up pasuje do vixy?", "Tak — oba style dzielą wysokie tempo i imprezową energię, dlatego często grane są w jednym secie.")))),
            Map.entry("pop", new Content(List.of(
                    "Pop to hity, które zna każdy — z radia, list przebojów i social mediów. Na imprezie sprawdzają się zwłaszcza w klubowych remiksach i wersjach extended.",
                    "Na VIXA znajdziesz popowe przeboje i ich taneczne wersje dodawane przez społeczność. Słuchaj za darmo i dodawaj je do składanek."),
                    List.of(new Faq("Czy pop pasuje na imprezę klubową?", "Tak, szczególnie w klubowych remiksach — znane refreny rozkręcają każdą salę."),
                            new Faq("Gdzie znaleźć klubowe remiksy popu?", "Na VIXA w zakładce Pop oraz w Dance i Club."),
                            new Faq("Jak dodać swój remiks?", "Załóż konto, kliknij „Dodaj utwór”, wybierz gatunek i wklej link do pliku z soundfiles.eu.")))),
            Map.entry("disco-polo", new Content(List.of(
                    "Disco polo to polska muzyka taneczna, bez której nie ma wesela, poprawin i domówki. Proste melodie, teksty o miłości i zabawie oraz rytm, przy którym tańczą wszyscy — od nastolatków po dziadków.",
                    "Na VIXA zbieramy nowości disco polo, remiksy i składanki na wesele. Słuchasz za darmo, możesz ocenić numer i ułożyć swoją listę na imprezę."),
                    List.of(new Faq("Jakie disco polo na wesele?", "Sprawdź ranking disco polo na VIXA — najwyżej są numery, przy których ludzie najchętniej się bawią."),
                            new Faq("Gdzie posłuchać nowości disco polo 2026?", "Na VIXA w zakładce Disco Polo i na miesięcznych stronach „Nowości disco polo”."),
                            new Faq("Czy są tu remiksy disco polo?", "Tak — obok oryginałów społeczność dodaje klubowe i vixowe remiksy znanych numerów."))))
    );
}
