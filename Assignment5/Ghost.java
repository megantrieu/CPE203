import java.util.List;
import processing.core.PImage;
import java.util.Optional;
import java.util.Random;
import java.util.NoSuchElementException;

public class Ghost extends MovingEntity {

    public static final String QUAKE_KEY = "quake";
    
    public Ghost(String id, Point position, List<PImage> images, int resourceLimit,
    int resourceCount, int actionPeriod, int animationPeriod) {
        super(id, position, images, resourceLimit, resourceCount, actionPeriod, animationPeriod);
    }

    public void executeGhostActivity(WorldModel world, ImageStore imageStore, EventScheduler scheduler) {
        MinerFull miner = new MinerFull(this.getId(), this.getPosition(), this.getImages(), 0, 0, this.getActionPeriod(), 0);
        MovingEntity target;
        try {
            target = (MinerFull)world.findNearest(this.getPosition(), miner).get();
        }
        catch (NoSuchElementException e) {
            try {
                MinerNotFull minerN = new MinerNotFull(this.getId(), this.getPosition(), this.getImages(), 0, 0, this.getActionPeriod(), 0);
                target = (MinerNotFull)world.findNearest(this.getPosition(), minerN).get();
            }
            catch (NoSuchElementException e2) {
                world.removeEntity((Entity)this);
                return;
            }
        }

        long nextPeriod = this.getActionPeriod();

        Point tgtPos = target.getPosition();

        if (moveToGhost(world, (Entity)target, scheduler)) {
            // make Miner disappear when ghost next to
            ActiveEntity quake = tgtPos.createQuake(imageStore.getImageList(QUAKE_KEY));

            world.addEntity((Entity)quake);
            nextPeriod += this.getActionPeriod();
            quake.scheduleActions(scheduler, world, imageStore);
        }

        Activity act = new Activity(this, world, imageStore, 0);
        scheduler.scheduleEvent(this,
        act.createActivityAction(world, imageStore),
        this.getActionPeriod());
    
    }


    public boolean moveToGhost(WorldModel world, Entity target, EventScheduler scheduler) {
        if (this.getPosition().adjacent(target.getPosition())) {
           world.removeEntity(target);
           scheduler.unscheduleAllEvents(target);
           return true;
        }
        else {
           Point nextPos = nextPosition(world, target.getPosition());
     
           if (!this.getPosition().equals(nextPos)) {
              Optional<Entity> occupant = world.getOccupant(nextPos);
              if (occupant.isPresent()) {
                 scheduler.unscheduleAllEvents(occupant.get());
              }
              world.moveEntity(this, nextPos);
           }
           return false;
        }
    }


    public void transformToGhost(Point p, WorldModel world, EventScheduler scheduler, ImageStore imageStore)
    {
        try {
            MinerFull miner = new MinerFull(this.getId(), this.getPosition(), this.getImages(), 0, 0, this.getActionPeriod(), 0);
            MinerFull died = (MinerFull)world.findNearest(this.getPosition(), miner).get();

            Entity ghost = (this.getPosition()).createGhost(this.getId(), this.resourceLimit,
            this.getActionPeriod(), this.getAnimationPeriod(), this.getImages());

            world.removeEntity(died);
            scheduler.unscheduleAllEvents(this);

            world.tryAddEntity(ghost);
            ActiveEntity g = (ActiveEntity)ghost;
            g.scheduleActions(scheduler, world, imageStore);
        }
        catch (Exception e){
            return;
        }

    }


}