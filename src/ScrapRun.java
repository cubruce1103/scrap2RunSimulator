import java.lang.Math;

class ScrapRun implements Cloneable{
    //Meta stats
    String upgradeOrder = "";
    boolean allowUpgradingFasterBarrel = true;
    boolean allowUpgradingDoubleBarrel = true;
    boolean allowUpgradingBetterBarrel = true;
    
    //Actual Variables
    int timeTotal = 0; // in centiseconds
    int timeSinceSpawn = 0;
    int timeSinceProduction = 0;
    
    double fieldMassExpected = 0; // assumes that barrels are instantly merged, fractional is ignored
    double reachedTier = -1; // 0-index: the default tier would be Tier 0
    int merge = 0;
    int[] tierMult = {1,2,4,8,12,16,48,96,320,1024};
    
    double scrapCurrent = 0; // using double because the search shouldn't go over 1e100
    double scrapTotal = 0;
    double scrapBoost = 1; // this should be something we can set to experiment
    final double goldenDifficulty = 1e8;

    int levelFasterBarrel = 0;
    int maxLevelFasterBarrel = 40;

    int levelDoubleBarrel = 0;
    int maxLevelDoubleBarrel = 50;

    int levelBetterBarrel = 0;

    boolean activatedFasterBarrelEvent = false;
    

    void addScrap(double amount){
        scrapCurrent += amount;
        scrapTotal += amount;
    }
    void minusScrap(double amount){
        scrapCurrent -= amount;
    }

    void countMerges(double massEx1, double massEx2){
        int mass1 = (int)massEx1;
        int mass2 = (int)massEx2;
        countMerges(mass1, mass2);
    }

    void countMerges(int mass1, int mass2){
        if (mass2 < mass1) {
            int temp = mass2;
            mass2 = mass1;
            mass1 = temp;
        }
        int count = 0;
        for(int i = 1; mass2>>i > 0; i++){
            count += (mass2>>i) - (mass1>>i);
        }
        merge += count;
    }

    double calculateGolden(){
        // calculating efficiency, ignore run multipliers, ignore discreteness of golden 

        // hack: because we are mapping (2**n - 1) to n (thanks geometric series), let's do log2(1+x) instead
        double scrapLog2 = Math.log1p( scrapTotal / goldenDifficulty) / Math.log(2);
        double goldenFromScrap = 10* (Math.floor(scrapLog2) + (Math.pow(2,scrapLog2 % 1f)-1) );// basic easier thing

        double goldenFromTier = tierMult[ (int) Math.max(0, Math.min((96-48)/6+1,(reachedTier-48)/6+1)) ]; // map 48 to 1 54 to 2 max to 9

        // This is where you do run multipliers like
        // goldenFromMagnetUpgrade, goldenFromMasteryBoost, goldenFromMasteryUpgrade, goldenFromBook, etc.

        double golden = goldenFromScrap * goldenFromTier; // and the rest
        return golden;
    }

    double costFasterBarrel(){
        return 1000 * Math.pow(2,levelFasterBarrel);
    }
    double costDoubleBarrel(){
        return 800_000 * Math.pow(2,levelDoubleBarrel);
    }
    double costBetterBarrel(){
        double cost = 20000* Math.pow(4, levelBetterBarrel);
        if(levelBetterBarrel>=15) cost *= 2; // TODO can defly use a Map for this
        if(levelBetterBarrel>=20) cost *= 2;
        if(levelBetterBarrel>=22) cost *= (15/8f);
        if(levelBetterBarrel>=25) cost *= (5/3f);
        if(levelBetterBarrel>=26) cost *= 2;
        if(levelBetterBarrel>=30) cost *= 2;
        if(levelBetterBarrel>=35) cost *= 2;
        if(levelBetterBarrel>=40) cost *= 2;
        if(levelBetterBarrel>=41) cost *= 2;
        if(levelBetterBarrel>=45) cost *= 4;
        if(levelBetterBarrel>=50) cost *= 4;
        if(levelBetterBarrel>=55) cost *= 8;
        if(levelBetterBarrel>=60) cost *= 8;
        if(levelBetterBarrel>=65) cost *= 8;
        if(levelBetterBarrel>=70) cost *= 8;
        if(levelBetterBarrel>=75) cost *= 8;
        if(levelBetterBarrel>=80) cost *= 8;
        if(levelBetterBarrel>=85) cost *= 8;
        if(levelBetterBarrel>=90) cost *= 2;
        if(levelBetterBarrel>=95) cost *= 2;
        if(levelBetterBarrel>=100) cost *= 2;
        if(levelBetterBarrel>=105) cost *= 2;
        if(levelBetterBarrel>=110) cost /= Math.pow(2, 1+Math.floor((levelBetterBarrel-110)/5));
        return cost;
    }

    boolean isDead(){
        if (allowUpgradingBetterBarrel) {
            return false;
        }
        if (allowUpgradingDoubleBarrel) {
            return false;
        }
        if (allowUpgradingFasterBarrel) {
            return false;
        }
        return true;
    }
    boolean isAbleToUpgradeFasterBarrel(){
        if (!allowUpgradingFasterBarrel){
            return false;
        }
        if(levelFasterBarrel == maxLevelFasterBarrel){
            return false;
        }
        if (scrapCurrent < costFasterBarrel()){
            return false;
        }
        return true;
    }
    boolean isAbleToUpgradeDoubleBarrel(){
        if (!allowUpgradingDoubleBarrel){
            return false;
        }
        if(levelDoubleBarrel == maxLevelDoubleBarrel){
            return false;
        }
        if (scrapCurrent < costDoubleBarrel()){
            return false;
        }
        return true;
    }
    boolean isAbleToUpgradeBetterBarrel(){
        if (!allowUpgradingBetterBarrel){
            return false;
        }
        if (scrapCurrent < costBetterBarrel()){
            return false;
        }
        return true;
    }

    void upgradeFasterBarrel(){
        if (!isAbleToUpgradeFasterBarrel()) {
            return; // or maybe even throw errors
        }
        minusScrap(costFasterBarrel());
        levelFasterBarrel += 1;
        upgradeOrder+="f";
    }
    void upgradeDoubleBarrel(){
        if (!isAbleToUpgradeDoubleBarrel()) {
            return; // or maybe even throw errors
        }
        minusScrap(costDoubleBarrel());
        levelDoubleBarrel += 1;
        upgradeOrder+="d";
        if(levelDoubleBarrel == maxLevelDoubleBarrel) allowUpgradingDoubleBarrel = false;
    }
    void upgradeBetterBarrel(){
        if (!isAbleToUpgradeBetterBarrel()) {
            return; // or maybe even throw errors
        }
        minusScrap(costBetterBarrel());
        levelBetterBarrel += 1;
        upgradeOrder+="B";

        double fractionMass = fieldMassExpected % 1f;
        double oldMass = Math.floor(fieldMassExpected)/2f;
        fieldMassExpected = Math.ceil(oldMass)+ fractionMass  ;
        if(levelBetterBarrel == maxLevelDoubleBarrel) allowUpgradingBetterBarrel = false;
        countMerges(oldMass, fieldMassExpected);
        updateReachedTier();
    }

    void produceScrap(){
        double scrapProduction = 0;
        int temp = (int)Math.floor(fieldMassExpected);
        int iter = 0;
        while(temp != 0){
            if (temp%2 ==1) scrapProduction += Math.pow(3,iter);
            temp /= 2;
            iter += 1;
        }
        scrapProduction *= scrapBoost;
        scrapProduction *= Math.pow(3, levelBetterBarrel);
        addScrap(scrapProduction);
    }
    void spawnBarrel(){
        double oldMass = fieldMassExpected;
        fieldMassExpected += (1 + levelDoubleBarrel/100d);
        countMerges(oldMass, fieldMassExpected);
        updateReachedTier();
    }

    void updateReachedTier(){
        int currentTier;
        if(fieldMassExpected < 1){ // if nothing is on field we are not reaching anything
            currentTier = -1;
        } else {
            currentTier = levelBetterBarrel + (int) Math.floor(Math.log(fieldMassExpected) / Math.log(2));
        }

        if(currentTier > reachedTier){
            reachedTier = currentTier;
        }
    }

    void timeAdvance(int duration){
        if(duration < 0){
            return; // no traveling back in time :3
        }
        timeTotal += duration;
        timeSinceSpawn += duration;
        timeSinceProduction += duration;
    }
    void stepAdvance(){
        // instead of going for the next event we stop at the next production
        int durationProductionCooldown = 100;
        int durationSpawnCooldown = ( 600 - levelFasterBarrel*10 );
        if(activatedFasterBarrelEvent){
            durationSpawnCooldown /= 10;
        }
        
        int timeUntilProduction = durationProductionCooldown - timeSinceProduction;
        int timeUntilSpawn = durationSpawnCooldown - timeSinceSpawn;
        
        while(timeUntilSpawn <= timeUntilProduction){ // spawn then produce
            timeAdvance(timeUntilSpawn);
            spawnBarrel();
            timeSinceSpawn = 0;
            timeUntilProduction = durationProductionCooldown - timeSinceProduction;
            timeUntilSpawn = durationSpawnCooldown - timeSinceSpawn;
        } 
        timeAdvance(timeUntilProduction);
        produceScrap();
        timeSinceProduction = 0;
    }
    
    ScrapRun(double scrapBoost, boolean activatedFasterBarrelEvent){
        this.scrapBoost = scrapBoost;
        this.activatedFasterBarrelEvent = activatedFasterBarrelEvent;

    }

    public ScrapRun clone() throws CloneNotSupportedException{
        return (ScrapRun)super.clone();
    }
}
