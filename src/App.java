/*
 * This code simulates runs with different upgrading orders in search of optimal progress
 * Each upgrade serve as a checkpoint and clones the simulation 
 * 
 * Assumptions im making:
 * only simulate the fastest going path (im greedy)
 * only buying FB, BB, DB
 * merges can be done immediately
 * upgrades can be bought immediately, in bulk
 * Should be true: there's no point to not upgrade immediately
 *
 * interesting cases:
 * w/o FB: 1e0, 1e21
 * w/  FB: ?
 */


import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;

public class App{
    static final int BETTERBARREL_LIMIT = 400;
    static final int TIME_LIMIT = 2 * 60 * 60 * 100;
    

    static ArrayList<ArrayList<ArrayList<ScrapRun>>> bestRuns = new ArrayList<ArrayList<ArrayList<ScrapRun>>>(100); // TODO with how it's done in this version we can reduce it to a 2dList
    static ArrayList<ScrapRun> pendingRuns = new ArrayList<ScrapRun>();
    static ArrayList<ScrapRun> nextRuns = new ArrayList<ScrapRun>();

    static String outputString = "";
    
    static void unlock(ScrapRun run){
        run.allowUpgradingBetterBarrel = true;
        run.allowUpgradingFasterBarrel = true;
        run.allowUpgradingDoubleBarrel = true;
    }
    
    static void addRun(ScrapRun run) throws CloneNotSupportedException{
        // make spaces for it
        for(int i = bestRuns.size(); i <= run.levelBetterBarrel; i++){
            bestRuns.add(new ArrayList<ArrayList<ScrapRun>>(41)); //[f:40][d:50]
            for( int j = 0; j <= 40; j++){
                bestRuns.get(i).add(new ArrayList<ScrapRun>(51));
                for( int k = 0; k<= 50; k++){
                    bestRuns.get(i).get(j).add(null);
                }
            }
        }
        //replace run and print if it's the fastest
        ScrapRun currentBest = bestRuns.get(run.levelBetterBarrel).get(run.levelFasterBarrel).get(run.levelDoubleBarrel);
        if (currentBest == null || currentBest.timeTotal > run.timeTotal) { // thanks, shortcutting!
            unlock(run);
            if(currentBest != null) nextRuns.remove(currentBest);
            nextRuns.add(run);
            bestRuns.get(run.levelBetterBarrel).get(run.levelFasterBarrel).set(run.levelDoubleBarrel, run);
        }
    }
    public static void main(String[] args) throws Exception {
        double scrapMult = 1;
        boolean activateFasterBarrel = false;
        try {
            scrapMult = Double.valueOf(args[0]);
            for (String arg : args) {
                if (arg.equals("-b")) {
                    activateFasterBarrel = true;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        String configurationString = String.format(
            "Run Configuration: %.1g x Scrap Multiplier%s\n", 
            scrapMult, ( activateFasterBarrel ? ", FB event activated" : "" )
            );
        System.out.printf(configurationString);

        ScrapRun initialRun = new ScrapRun(scrapMult, activateFasterBarrel);
        addRun(initialRun);
        int iter = 0;
        
        do {
            for (ScrapRun scrapRun : pendingRuns) {
                while (true) {
                    if (scrapRun.isDead() || scrapRun.levelBetterBarrel > BETTERBARREL_LIMIT || scrapRun.timeTotal > TIME_LIMIT){
                        break;
                    }
                    if (scrapRun.isAbleToUpgradeBetterBarrel()) {
                        ScrapRun clonedRun = scrapRun.clone();
                        clonedRun.upgradeBetterBarrel();
                        addRun(clonedRun);
                        scrapRun.allowUpgradingBetterBarrel = false;
                    }
                    if (scrapRun.isAbleToUpgradeFasterBarrel()) {
                        ScrapRun clonedRun = scrapRun.clone();
                        clonedRun.upgradeFasterBarrel();
                        addRun(clonedRun);
                        scrapRun.allowUpgradingFasterBarrel = false;
                    }
                    if (scrapRun.isAbleToUpgradeDoubleBarrel()) {
                        ScrapRun clonedRun = scrapRun.clone();
                        clonedRun.upgradeDoubleBarrel();
                        addRun(clonedRun);
                        scrapRun.allowUpgradingDoubleBarrel = false;
                    }
                    scrapRun.stepAdvance();
                }
            }

            pendingRuns.clear();
            for (ScrapRun scrapRun : nextRuns) {
                pendingRuns.add(scrapRun.clone());
            }
            pendingRuns.sort(new Comparator<ScrapRun>() {
                    @Override
                    public int compare(ScrapRun o1, ScrapRun o2) {
                        double a = o1.calculateGolden()/o1.timeTotal;
                        double b = o2.calculateGolden()/o2.timeTotal;
                        if (a > b) {
                            return -1;
                        } 
                        else if ( a < b ){
                            return +1;
                        } 
                        else {
                            return 0;
                        }
                    }
                }
                );
                nextRuns.clear();
                for (ScrapRun scrapRun : pendingRuns.subList(0, Math.min(1, pendingRuns.size()))) {
                //000[0000], (  0; 0; 0), 000.00 GS / 0000 sec, 00000 merges, 0.0000 GS/hr, "B"
                outputString += String.format("%3d[%4d], (%3d;%2d;%2d), %8.0f GS / %4d sec, %5d merges, %12.4f GS/hr, \"%s\"\n",
                    iter, pendingRuns.size(),
                    scrapRun.levelBetterBarrel, scrapRun.levelFasterBarrel, scrapRun.levelDoubleBarrel,
                    scrapRun.calculateGolden(), scrapRun.timeTotal/100, 
                    scrapRun.merge,
                    scrapRun.calculateGolden()/((double)scrapRun.timeTotal/(100 * 3600)),
                    scrapRun.upgradeOrder.substring(0, Math.min(120,scrapRun.upgradeOrder.length()))
                    );
                iter ++;
            }
        } while(pendingRuns.size() > 0);

        //write to file
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu_MMdd_HHmmss");
        String filename = "out" + LocalDateTime.now(ZoneOffset.of("Z")).format(formatter).toString() + ".txt";
        FileWriter writer = new FileWriter(filename);
        writer.write(outputString);
        writer.close();
    }
}
