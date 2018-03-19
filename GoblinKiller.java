package GoblinKiller;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.rt4.*;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Script.Manifest(

        name="Goblin Killer",
        description = "Kills Goblins",
        properties = "client=4"
)

public class GoblinKiller extends PollingScript<ClientContext>
{
    Component inventory = ctx.widgets.widget(161).component(61);

    int pollCount = 0;
    final static int GOBLIN[] = { 3029, 3030, 3031, 3032, 3034, 3035 };
    final static int FOOD[] = { 315, 1971, 2309 };

    final static int HUT_VALUES[] = {3243, 3244, 3245, 3246, 3247, 3248};


    long startTime;
    long endTime;
    long runTime;

    @Override
    public void start()
    {
        startTime = System.currentTimeMillis();
        System.out.println("Starting.");
    }

    @Override
    public void stop()
    {
        endTime = System.currentTimeMillis();
        runTime = (endTime - startTime) / 1000;
        System.out.printf("Total Runtime: %ds\n", runTime);
        System.out.printf("Polls: %d\n", pollCount);
        System.out.printf("Polls per second: %d\n", pollCount/runTime);
        System.out.println("Stopped.");
    }

    public boolean needsHeal()
    {
        return ctx.combat.health() < 6;
    }

    @Override
    public void poll()
    {
        if(bonesInInventory() && !ctx.players.local().inCombat())
            dropBones();

        while(lootNearby() && !ctx.players.local().inCombat())
        {
            pickup();
        }

        if(hasFood())
        {
            if(needsHeal())
            {
                heal();
            }
            else if(shouldAttack())
            {
                attack();
            }
        }
        pollCount += 1;
    }

    public boolean bonesInInventory()
    {
        return ctx.inventory.select().id(526).count() > 0;
    }

    public boolean lootNearby()
    {
        GroundItem loot = ctx.groundItems.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).nearest().poll();
        return(loot.tile().distanceTo(ctx.players.local().tile()) <= 4);
    }

    public void pickup()
    {
        GroundItem loot = ctx.groundItems.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).nearest().poll();

       if(ctx.inventory.select().id(loot).count() > 0 || ctx.inventory.count() < 28)
            loot.interact("Take");

        final int startingWealth = ctx.inventory.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).count(true);

        Condition.wait(new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                final int currentWealth = ctx.inventory.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).count(true);
                return currentWealth != startingWealth;
            }
        }, 150, 15);
    }

    public boolean shouldAttack()
    {
        return !ctx.players.local().inCombat();
    }

    public boolean hasFood()
    {
        return ctx.inventory.select().id(FOOD).count() > 0;
    }

    public void attack()
    {
        System.out.printf("Selecting A Goblin To Attack\n");
        final Npc goblin = ctx.npcs.select().id(GOBLIN).select(new Filter<Npc>()
        {
            @Override
            public boolean accept(Npc npc)
            {
                return !npc.inCombat();
            }
        }).select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc)
            {
                boolean hit = false;
                int x = npc.tile().x();
                int y = npc.tile().y();
                for(int i = 0;  i < HUT_VALUES.length; i++ )
                {
                    if(x == HUT_VALUES[i])
                        for(int j = 0; j < HUT_VALUES.length; j++)
                            if(y == HUT_VALUES[j])
                                hit = true;
                }
                return hit == false;
            }
        }).select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc)
            {
                int count = 0;
                int y = npc.tile().y();
                for(int i = 0;  i < HUT_VALUES.length; i++ )
                {
                    if(y == HUT_VALUES[i])
                        count += 1;
                }
                return count == 0;
            }
        }).nearest().poll();

        if(!goblin.inViewport());
        ctx.camera.turnTo(goblin);


        goblin.interact("Attack", "Goblin");

        Condition.wait(new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                //System.out.printf("Player In Combat, Waiting...\n");
                return ctx.players.local().inCombat();
            }
        }, 200, 20);
    }

    public void heal()
    {
        inventory.click();

        Item food = ctx.inventory.select().id(FOOD).poll();

        food.interact("Eat");

        final int startingHealth = ctx.combat.health();

        Condition.wait(new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                final int currentHealth = ctx.combat.health();
                return currentHealth != startingHealth;
            }
        }, 150, 20);
    }

    public void dropBones()
    {
        for(Item bones : ctx.inventory.select().id(526))
        {
            if(ctx.controller.isStopping())
            {
                break;
            }

            final int startAmountInventory = ctx.inventory.select().count();
            bones.interact("Drop", "Bones");

            Condition.wait(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    return ctx.inventory.select().count() != startAmountInventory;
                }
            }, 75, 20);
        }
    }
}
