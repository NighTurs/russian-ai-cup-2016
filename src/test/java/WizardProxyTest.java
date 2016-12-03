import model.Game;
import model.SkillType;
import model.Status;
import model.StatusType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WizardProxyTest {

    private static final List<SkillType> EMPTY_SKILLS = Collections.emptyList();
    private static final Status[] EMPTY_STATUSES = new Status[0];
    private Game game;
    private WizardProxy wizard;

    @Before
    public void setUp() throws Exception {
        game = mock(Game.class);
        wizard = mock(WizardProxy.class);
    }

    @Test
    public void testGetMagicMissileDirectDamageRegular() throws Exception {
        when(game.getMagicMissileDirectDamage()).thenReturn(12);
        when(game.getMagicalDamageBonusPerSkillLevel()).thenReturn(1);
        when(game.getEmpoweredDamageFactor()).thenReturn(0.5);
        when(wizard.affectedBySkills()).thenReturn(EMPTY_SKILLS);
        when(wizard.getStatuses()).thenReturn(EMPTY_STATUSES);
        assertEquals(12.0, WizardProxy.getMagicMissileDirectDamage(wizard, game), 0.0);
    }

    @Test
    public void testGetMagicMissileDirectDamageSkillsAndBonus() throws Exception {
        when(game.getMagicMissileDirectDamage()).thenReturn(12);
        when(game.getMagicalDamageBonusPerSkillLevel()).thenReturn(1);
        when(game.getEmpoweredDamageFactor()).thenReturn(0.5);
        when(wizard.affectedBySkills()).thenReturn(Arrays.asList(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2));
        when(wizard.getStatuses()).thenReturn(new Status[]{new Status(0, StatusType.EMPOWERED, 0, 0, 1)});
        assertEquals(24.0, WizardProxy.getMagicMissileDirectDamage(wizard, game), 0.0);
    }

    @Test
    public void testGetWizardForwardSpeedRegular() throws Exception {
        when(game.getWizardForwardSpeed()).thenReturn(4.0);
        when(game.getHastenedMovementBonusFactor()).thenReturn(0.3);
        when(game.getMovementBonusFactorPerSkillLevel()).thenReturn(0.05);
        when(wizard.affectedBySkills()).thenReturn(EMPTY_SKILLS);
        when(wizard.getStatuses()).thenReturn(EMPTY_STATUSES);
        assertEquals(4.0, WizardProxy.getWizardForwardSpeed(wizard, game), 0.0);
    }

    @Test
    public void testGetWizardForwardSpeedSkillsAndBonus() throws Exception {
        when(game.getWizardForwardSpeed()).thenReturn(4.0);
        when(game.getHastenedMovementBonusFactor()).thenReturn(0.3);
        when(game.getMovementBonusFactorPerSkillLevel()).thenReturn(0.05);
        when(wizard.affectedBySkills()).thenReturn(Arrays.asList(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2));
        when(wizard.getStatuses()).thenReturn(new Status[]{new Status(0, StatusType.HASTENED, 0, 0, 1)});
        assertEquals(6.0, WizardProxy.getWizardForwardSpeed(wizard, game), 0.0);
    }

    @Test
    public void testGetWizardManaPerTurn() throws Exception {
        when(game.getWizardBaseManaRegeneration()).thenReturn(0.2);
        when(game.getWizardManaRegenerationGrowthPerLevel()).thenReturn(0.02);
        when(wizard.getLevel()).thenReturn(5);
        assertEquals(0.3, WizardProxy.getWizardManaPerTurn(wizard, game), 1e-9);
    }
}